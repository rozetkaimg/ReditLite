package com.rozetka.domain.usecase.feed

import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.Post
import com.rozetka.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow

class ObserveFeedUseCase(
    private val feedRepository: FeedRepository
) {
    operator fun invoke(feedType: FeedType): Flow<List<Post>> {
        return feedRepository.getFeedFlow(feedType)
    }
}
