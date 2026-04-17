package com.rozetka.domain.usecase.subreddit

import com.rozetka.domain.repository.SubredditRepository

class GetSubredditRulesUseCase(
    private val subredditRepository: SubredditRepository
) {
    suspend operator fun invoke(subredditName: String): Result<List<String>> {
        return subredditRepository.getSubredditRules(subredditName)
    }
}
