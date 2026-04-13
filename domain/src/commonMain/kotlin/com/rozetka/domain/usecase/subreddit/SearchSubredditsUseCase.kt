package com.rozetka.domain.usecase.subreddit

import com.rozetka.domain.model.Subreddit
import com.rozetka.domain.repository.SubredditRepository

class SearchSubredditsUseCase(
    private val subredditRepository: SubredditRepository
) {
    suspend operator fun invoke(query: String): Result<List<Subreddit>> {
        if (query.isBlank()) return Result.success(emptyList())
        return subredditRepository.searchSubreddits(query)
    }
}
