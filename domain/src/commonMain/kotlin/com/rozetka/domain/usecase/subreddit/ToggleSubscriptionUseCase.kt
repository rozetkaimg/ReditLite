package com.rozetka.domain.usecase.subreddit

import com.rozetka.domain.repository.SubredditRepository

class ToggleSubscriptionUseCase(
    private val subredditRepository: SubredditRepository
) {
    suspend operator fun invoke(subredditName: String, isCurrentlySubscribed: Boolean): Result<Unit> {
        return if (isCurrentlySubscribed) {
            subredditRepository.unsubscribe(subredditName)
        } else {
            subredditRepository.subscribe(subredditName)
        }
    }
}
