package com.rozetka.domain.usecase.subreddit

import com.rozetka.domain.model.Subreddit
import com.rozetka.domain.repository.SubredditRepository

class GetSubredditInfoUseCase(
    private val subredditRepository: SubredditRepository
) {
    suspend operator fun invoke(subredditName: String): Result<Subreddit> {
        return subredditRepository.getSubredditInfo(subredditName)
    }
}
