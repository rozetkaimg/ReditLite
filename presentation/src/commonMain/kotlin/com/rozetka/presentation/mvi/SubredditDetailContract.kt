package com.rozetka.presentation.mvi

import com.rozetka.domain.model.Post
import com.rozetka.domain.model.Subreddit

data class SubredditDetailState(
    val subreddit: Subreddit? = null,
    val posts: List<Post> = emptyList(),
    val searchResults: List<Post> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
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
    data class VotePost(val postId: String, val direction: com.rozetka.domain.model.VoteDirection) : SubredditDetailIntent()
    data class SavePost(val postId: String) : SubredditDetailIntent()
    data class SearchPosts(val query: String) : SubredditDetailIntent()
    data class SetSearchActive(val active: Boolean) : SubredditDetailIntent()
}
