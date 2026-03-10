package com.rozetka.reditlite.data



import com.rozetka.reditlite.models.CommentItem

sealed interface PostDetailState {
    data object Loading : PostDetailState
    data class Content(val comments: List<CommentItem>) : PostDetailState
    data class Error(val message: String) : PostDetailState
}