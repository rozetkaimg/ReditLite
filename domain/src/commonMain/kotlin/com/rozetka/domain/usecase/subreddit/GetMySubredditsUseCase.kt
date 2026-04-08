package com.rozetka.domain.usecase.subreddit

import com.rozetka.domain.model.Subreddit
import com.rozetka.domain.repository.SubredditRepository

class GetMySubredditsUseCase(
    private val repository: SubredditRepository
) {
    suspend operator fun invoke(): Result<List<Subreddit>> {
        return repository.fetchMySubreddits()
    }
}