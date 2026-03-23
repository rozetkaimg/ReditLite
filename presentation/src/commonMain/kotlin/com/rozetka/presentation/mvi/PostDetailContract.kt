package com.rozetka.presentation.mvi

import com.rozetka.domain.model.Comment

sealed interface PostDetailState {
    data object Loading : PostDetailState
    data class Content(val comments: List<Comment>) : PostDetailState
    data class Error(val message: String) : PostDetailState
}

sealed interface PostDetailIntent {
    data class LoadComments(val postId: String) : PostDetailIntent
}
