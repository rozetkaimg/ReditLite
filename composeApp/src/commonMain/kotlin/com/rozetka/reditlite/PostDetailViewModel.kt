package com.rozetka.reditlite

import com.rozetka.reditlite.data.PostDetailState


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.reditlite.data.RedditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val repository: RedditRepository = RedditRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val state: StateFlow<PostDetailState> = _state.asStateFlow()

    fun loadComments(postId: String) {
        _state.value = PostDetailState.Loading
        viewModelScope.launch {
            repository.getComments("all", postId).fold(
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