package com.rozetka.domain.usecase.subreddit

import com.rozetka.domain.model.Subreddit
import com.rozetka.domain.repository.SubredditRepository
import kotlinx.coroutines.flow.Flow

class ObserveMySubredditsUseCase(
    private val repository: SubredditRepository
) {
    operator fun invoke(): Flow<List<Subreddit>> {
        return repository.getSubredditsFlow()
    }
}
