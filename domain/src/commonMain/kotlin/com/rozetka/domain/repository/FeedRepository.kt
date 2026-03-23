package com.rozetka.domain.repository


import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.PaginatedData
import com.rozetka.domain.model.Post
import kotlinx.coroutines.flow.Flow


interface FeedRepository {
    fun getFeedFlow(feedType: FeedType): Flow<List<Post>>
    suspend fun fetchFeed(feedType: FeedType, after: String?, limit: Int = 25): Result<PaginatedData<Post>>
}
