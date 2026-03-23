package com.rozetka.data.repository

import com.rozetka.data.local.AppDatabase
import com.rozetka.data.mapper.toDomain
import com.rozetka.data.mapper.toEntity
import com.rozetka.data.model.remote.RedditListingResponse
import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.PaginatedData
import com.rozetka.domain.model.Post
import com.rozetka.domain.repository.FeedRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedRepositoryImpl(
    private val client: HttpClient,
    private val database: AppDatabase
) : FeedRepository {

    override fun getFeedFlow(feedType: FeedType): Flow<List<Post>> {
        return database.postDao().getPostsByFeedType(feedType.path).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchFeed(feedType: FeedType, after: String?, limit: Int): Result<PaginatedData<Post>> {
        return runCatching {
            val response: RedditListingResponse = client.get("https://oauth.reddit.com/${feedType.path}") {
                parameter("limit", limit)
                if (after != null) parameter("after", after)
            }.body()

            val networkPosts = response.data.children.map { it.data }
            val posts = networkPosts.map { it.toDomain() }
            val entities = networkPosts.map { it.toEntity(feedType.path) }

            if (after == null) {
                database.postDao().clearFeed(feedType.path)
            }
            database.postDao().insertPosts(entities)

            PaginatedData(
                items = posts,
                after = response.data.after,
                before = response.data.before
            )
        }
    }
}