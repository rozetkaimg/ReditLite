package com.rozetka.presentation.mvi

import com.rozetka.domain.model.Post
import com.rozetka.domain.model.Subreddit

data class FavoritesState(
    val savedPosts: List<Post> = emptyList(),
    val favoriteSubreddits: List<Subreddit> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface FavoritesIntent {
    object LoadFavorites : FavoritesIntent
    data class TogglePostSave(val post: Post) : FavoritesIntent
    data class ToggleSubredditFavorite(val subreddit: Subreddit) : FavoritesIntent
}
