package com.rozetka.domain.usecase.feed

import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.PaginatedData
import com.rozetka.domain.model.Post
import com.rozetka.domain.repository.FeedRepository

class FetchSubredditFeedUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(
        subredditName: String,
        after: String? = null,
        limit: Int = 25
    ): Result<PaginatedData<Post>> {
        return feedRepository.fetchFeed(FeedType.Subreddit(subredditName), after, limit)
    }
}
