package com.rozetka.presentation.ui.screen.postDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.Comment
import com.rozetka.domain.usecase.post.GetCommentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PostDetailState {
    object Loading : PostDetailState
    data class Content(val comments: List<Comment>) : PostDetailState
    data class Error(val message: String) : PostDetailState
}

class PostDetailViewModel(
    private val getCommentsUseCase: GetCommentsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val state: StateFlow<PostDetailState> = _state.asStateFlow()

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _state.value = PostDetailState.Loading
            getCommentsUseCase(postId).fold(
                onSuccess = { comments ->
                    _state.value = PostDetailState.Content(comments)
                },
                onFailure = { error ->
                    _state.value = PostDetailState.Error(error.message ?: "Failed to load comments")
                }
            )
        }
    }
}