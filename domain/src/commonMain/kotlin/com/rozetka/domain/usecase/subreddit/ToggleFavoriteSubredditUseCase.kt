package com.rozetka.domain.usecase.subreddit

import com.rozetka.domain.repository.SubredditRepository

class ToggleFavoriteSubredditUseCase(
    private val repository: SubredditRepository
) {
    suspend operator fun invoke(subredditName: String, isFavorite: Boolean) {
        repository.toggleFavorite(subredditName, isFavorite)
    }
}
