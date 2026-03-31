package com.rozetka.data.repository

import com.rozetka.data.local.SubredditDao
import com.rozetka.data.mapper.toDomain
import com.rozetka.data.mapper.toSubredditDomain
import com.rozetka.data.mapper.toSubredditEntity
import com.rozetka.data.model.remote.RedditListingResponse
import com.rozetka.data.model.remote.SubredditAboutResponse
import com.rozetka.data.model.remote.SubredditRulesResponse
import com.rozetka.domain.model.Subreddit
import com.rozetka.domain.repository.SubredditRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubredditRepositoryImpl(
    private val client: HttpClient,
    private val subredditDao: SubredditDao
) : SubredditRepository {

    override fun getSubredditsFlow(): Flow<List<Subreddit>> {
        return subredditDao.getSubreddits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteSubredditsFlow(): Flow<List<Subreddit>> {
        return subredditDao.getFavoriteSubreddits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchMySubreddits(): Result<List<Subreddit>> {
        return runCatching {
            val response: RedditListingResponse = client.get("https://oauth.reddit.com/subreddits/mine/subscriber") {
                parameter("limit", 100)
            }.body()

            val subreddits = response.data.children.map { it.data.toSubredditDomain() }
            val entities = response.data.children.map { it.data.toSubredditEntity() }
            
            subredditDao.insertSubreddits(entities)
            
            subreddits
        }
    }

    override suspend fun getSubredditInfo(subredditName: String): Result<Subreddit> {
        return runCatching {
            val response: SubredditAboutResponse = client.get("https://oauth.reddit.com/r/$subredditName/about").body()
            val data = response.data
            data.toSubredditDomain()
        }
    }

    override suspend fun getSubredditRules(subredditName: String): Result<List<String>> {
        return runCatching {
            val response: SubredditRulesResponse = client.get("https://oauth.reddit.com/r/$subredditName/about/rules").body()
            response.rules.map { it.short_name }
        }
    }

    override suspend fun subscribe(subredditName: String): Result<Unit> {
        return runCatching {
            client.post("https://oauth.reddit.com/api/subscribe") {
                parameter("action", "sub")
                parameter("sr_name", subredditName)
            }
            subredditDao.updateSubscriptionStatus(subredditName, true)
        }
    }

    override suspend fun unsubscribe(subredditName: String): Result<Unit> {
        return runCatching {
            client.post("https://oauth.reddit.com/api/subscribe") {
                parameter("action", "unsub")
                parameter("sr_name", subredditName)
            }
            subredditDao.updateSubscriptionStatus(subredditName, false)
        }
    }

    override suspend fun toggleFavorite(subredditName: String, isFavorite: Boolean) {
        subredditDao.updateFavoriteStatus(subredditName, isFavorite)
    }
}