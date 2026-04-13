package com.rozetka.presentation.mvi

import com.rozetka.domain.model.Subreddit

data class SubredditsState(
    val subreddits: List<Subreddit> = emptyList(),
    val searchResults: List<Subreddit> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class SubredditsIntent {
    object LoadSubreddits : SubredditsIntent()
    data class SearchSubreddits(val query: String) : SubredditsIntent()
    data class ToggleSubscription(val subreddit: Subreddit) : SubredditsIntent()
    data class ToggleFavorite(val subreddit: Subreddit) : SubredditsIntent()
    data class NavigateToSubreddit(val name: String) : SubredditsIntent()
}

sealed class SubredditsEffect {
    data class ShowError(val message: String) : SubredditsEffect()
    data class NavigateToSubreddit(val name: String) : SubredditsEffect()
}
