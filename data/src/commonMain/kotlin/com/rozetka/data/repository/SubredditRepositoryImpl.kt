package com.rozetka.data.repository

import com.rozetka.data.mapper.toSubredditDomain
import com.rozetka.data.model.remote.RedditListingResponse
import com.rozetka.domain.model.Subreddit
import com.rozetka.domain.repository.SubredditRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

class SubredditRepositoryImpl(
    private val client: HttpClient
) : SubredditRepository {

    override suspend fun fetchMySubreddits(): Result<List<Subreddit>> {
        return runCatching {
            val response: RedditListingResponse = client.get("https://oauth.reddit.com/subreddits/mine/subscriber") {
                parameter("limit", 50)
            }.body()

            response.data.children.map { it.data.toSubredditDomain() }
        }
    }

    override suspend fun getSubredditInfo(subredditName: String): Result<Subreddit> {
        return runCatching {
            val response: RedditListingResponse = client.get("https://oauth.reddit.com/r/$subredditName/about").body()
            val data = response.data.children.firstOrNull()?.data ?: throw Exception("Not found")
            data.toSubredditDomain()
        }
    }

    override suspend fun subscribe(subredditName: String): Result<Unit> {
        return runCatching {
            client.post("https://oauth.reddit.com/api/subscribe") {
                parameter("action", "sub")
                parameter("sr_name", subredditName)
            }
        }
    }

    override suspend fun unsubscribe(subredditName: String): Result<Unit> {
        return runCatching {
            client.post("https://oauth.reddit.com/api/subscribe") {
                parameter("action", "unsub")
                parameter("sr_name", subredditName)
            }
        }
    }
}