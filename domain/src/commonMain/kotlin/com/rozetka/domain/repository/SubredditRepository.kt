package com.rozetka.domain.repository

import com.rozetka.domain.model.Subreddit

interface SubredditRepository {
    suspend fun fetchMySubreddits(): Result<List<Subreddit>>
    suspend fun getSubredditInfo(subredditName: String): Result<Subreddit>
    suspend fun subscribe(subredditName: String): Result<Unit>
    suspend fun unsubscribe(subredditName: String): Result<Unit>
}
