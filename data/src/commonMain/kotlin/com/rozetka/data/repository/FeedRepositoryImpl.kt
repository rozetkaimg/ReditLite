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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FeedRepositoryImpl(
    private val client: HttpClient,
    private val database: AppDatabase
) : FeedRepository {

    override fun getFeedFlow(feedType: FeedType): Flow<List<Post>> {
        return if (feedType == FeedType.SAVED) {
            database.postDao().getSavedPosts().map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            database.postDao().getPostsByFeedType(feedType.path).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override suspend fun fetchFeed(feedType: FeedType, after: String?, limit: Int): Result<PaginatedData<Post>> {
        if (feedType == FeedType.SAVED) {
            return Result.success(PaginatedData(emptyList(), null, null))
        }
        return runCatching {
            val response: RedditListingResponse = client.get("https://oauth.reddit.com/${feedType.path}") {
                parameter("limit", limit)
                if (after != null) parameter("after", after)
            }.body()

            val networkPosts = response.data.children.map { it.data }
            
            val startOrderIndex = if (after == null) {
                database.postDao().clearFeed(feedType.path)
                0
            } else {
                (database.postDao().getMaxOrderIndex(feedType.path) ?: -1) + 1
            }

            val entities = networkPosts.mapIndexed { index, networkPost -> 
                networkPost.toEntity(feedType.path, startOrderIndex + index)
            }
            
            // Preserve isSaved status for existing posts
            val savedPostIds = database.postDao().getSavedPosts().first().map { it.id }.toSet()
            val entitiesWithSavedStatus = entities.map { entity ->
                if (savedPostIds.contains(entity.id)) {
                    entity.copy(isSaved = true)
                } else {
                    entity
                }
            }
            
            database.postDao().insertPosts(entitiesWithSavedStatus)

            val posts = entitiesWithSavedStatus.map { it.toDomain() }

            PaginatedData(
                items = posts,
                after = response.data.after,
                before = response.data.before
            )
        }
    }

    override suspend fun searchPosts(query: String, after: String?, limit: Int): Result<PaginatedData<Post>> {
        return runCatching {
            val response: RedditListingResponse = client.get("https://oauth.reddit.com/search") {
                parameter("q", query)
                parameter("limit", limit)
                if (after != null) parameter("after", after)
            }.body()

            val posts = response.data.children.map { it.data.toDomain() }

            PaginatedData(
                items = posts,
                after = response.data.after,
                before = response.data.before
            )
        }
    }
}