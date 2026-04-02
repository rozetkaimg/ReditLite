package com.rozetka.presentation.ui.screen.postCreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.repository.PostRepository
import com.rozetka.domain.work.PostWorkManager
import com.rozetka.presentation.mvi.PostCreationContract
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostCreationViewModel(
    private val repository: PostRepository,
    private val postWorkManager: PostWorkManager
) : ViewModel() {

    private val _state = MutableStateFlow(PostCreationContract.State())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PostCreationContract.Effect>()
    val effect = _effect.asSharedFlow()

    fun handleEvent(event: PostCreationContract.Event) {
        when (event) {
            is PostCreationContract.Event.OnSubredditSelected -> {
                _state.update { it.copy(selectedSubreddit = event.subreddit) }
            }
            is PostCreationContract.Event.OnTitleChanged -> {
                _state.update { it.copy(title = event.title) }
            }
            is PostCreationContract.Event.OnContentChanged -> {
                _state.update { it.copy(content = event.content) }
            }
            is PostCreationContract.Event.OnPostTypeChanged -> {
                _state.update { it.copy(postType = event.type) }
            }
            is PostCreationContract.Event.OnMediaSelected -> {
                _state.update { it.copy(mediaUri = event.uri) }
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

        // Use WorkManager for background submission
        postWorkManager.enqueuePostSubmission(
            subreddit = currentState.selectedSubreddit,
            title = currentState.title,
            content = currentState.content,
            type = currentState.postType.name,
            mediaUri = currentState.mediaUri
        )
        
        _state.update { it.copy(isSuccess = true) }
    }
}
