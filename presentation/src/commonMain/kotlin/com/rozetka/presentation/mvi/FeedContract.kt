package com.rozetka.presentation.mvi

import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.Post

sealed interface FeedState {
    data object Loading : FeedState
    data class Content(
        val posts: List<Post>,
        val isPaginating: Boolean = false,
        val nextAfter: String? = null
    ) : FeedState
    data class Error(val message: String) : FeedState
    data object Empty : FeedState
}

sealed interface FeedIntent {
    data class LoadInitial(val feedType: FeedType) : FeedIntent
    data class Refresh(val feedType: FeedType) : FeedIntent
    data class LoadNextPage(val feedType: FeedType) : FeedIntent
}
