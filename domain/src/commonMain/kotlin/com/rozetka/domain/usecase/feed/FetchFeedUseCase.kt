package com.rozetka.domain.usecase.feed

import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.PaginatedData
import com.rozetka.domain.model.Post
import com.rozetka.domain.repository.FeedRepository

class FetchFeedUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(feedType: FeedType, after: String?, limit: Int = 25): Result<PaginatedData<Post>> {
        return feedRepository.fetchFeed(feedType, after, limit)
    }
}
