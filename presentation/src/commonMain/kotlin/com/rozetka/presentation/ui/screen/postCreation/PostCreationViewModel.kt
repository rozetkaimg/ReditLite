package com.rozetka.presentation.ui.screen.postCreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.usecase.submit.SubmitImagePostWithBytesUseCase
import com.rozetka.domain.usecase.submit.SubmitLinkPostUseCase
import com.rozetka.domain.usecase.submit.SubmitTextPostUseCase
import com.rozetka.domain.work.PostWorkManager
import com.rozetka.presentation.mvi.PostCreationContract
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostCreationViewModel(
    private val submitTextPostUseCase: SubmitTextPostUseCase,
    private val submitLinkPostUseCase: SubmitLinkPostUseCase,
    private val submitImagePostWithBytesUseCase: SubmitImagePostWithBytesUseCase,
    private val postWorkManager: PostWorkManager
) : ViewModel() {

    private val _state = MutableStateFlow(PostCreationContract.State())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PostCreationContract.Effect>()
    val effect = _effect.asSharedFlow()

    fun handleEvent(event: PostCreationContract.Event) {
        when (event) {
            is PostCreationContract.Event.OnSubredditSelected -> {
                _state.update { it.copy(selectedSubreddit = event.subreddit, error = null) }
            }
            is PostCreationContract.Event.OnTitleChanged -> {
                _state.update { it.copy(title = event.title, error = null) }
            }
            is PostCreationContract.Event.OnContentChanged -> {
                _state.update { it.copy(content = event.content, error = null) }
            }
            is PostCreationContract.Event.OnPostTypeChanged -> {
                _state.update { it.copy(postType = event.type, error = null) }
            }
            is PostCreationContract.Event.OnMediaSelected -> {
                _state.update {
                    it.copy(
                        mediaBytes = event.bytes,
                        fileName = event.fileName,
                        error = null
                    )
                }
            }
            PostCreationContract.Event.OnSubmitClicked -> submitPost()
            PostCreationContract.Event.OnDismissError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun submitPost() {
        val currentState = _state.value
        if (currentState.selectedSubreddit.isBlank() || currentState.title.isBlank()) {
            _state.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = when (currentState.postType) {
                PostCreationContract.PostType.TEXT -> {
                    submitTextPostUseCase(
                        subreddit = currentState.selectedSubreddit,
                        title = currentState.title,
                        text = currentState.content
                    )
                }

                PostCreationContract.PostType.LINK -> {
                    submitLinkPostUseCase(
                        subreddit = currentState.selectedSubreddit,
                        title = currentState.title,
                        url = currentState.content
                    )
                }

                PostCreationContract.PostType.IMAGE -> {
                    val bytes = currentState.mediaBytes
                    val fileName = currentState.fileName
                    if (bytes != null && fileName != null) {
                        submitImagePostWithBytesUseCase(
                            subreddit = currentState.selectedSubreddit,
                            title = currentState.title,
                            imageBytes = bytes,
                            fileName = fileName
                        )
                    } else {
                        Result.failure(Exception("Please select an image"))
                    }
                }
            }

            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
            }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to create post") }
                }
        }
    }
}
