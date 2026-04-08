package com.rozetka.presentation.mvi

import com.rozetka.domain.model.Post
import com.rozetka.domain.model.Subreddit

data class SubredditDetailState(
    val subreddit: Subreddit? = null,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isPaginating: Boolean = false,
    val error: String? = null,
    val after: String? = null
)

sealed class SubredditDetailIntent {
    data class LoadSubreddit(val name: String) : SubredditDetailIntent()
    object Refresh : SubredditDetailIntent()
    object LoadMore : SubredditDetailIntent()
    object ToggleSubscription : SubredditDetailIntent()
}
