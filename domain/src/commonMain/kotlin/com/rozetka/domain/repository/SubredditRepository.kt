package com.rozetka.domain.repository

import com.rozetka.domain.model.Subreddit
import kotlinx.coroutines.flow.Flow

interface SubredditRepository {
    fun getSubredditsFlow(): Flow<List<Subreddit>>
    fun getFavoriteSubredditsFlow(): Flow<List<Subreddit>>
    suspend fun fetchMySubreddits(): Result<List<Subreddit>>
    suspend fun searchSubreddits(query: String): Result<List<Subreddit>>
    suspend fun getSubredditInfo(subredditName: String): Result<Subreddit>
    suspend fun getSubredditRules(subredditName: String): Result<List<String>>
    suspend fun subscribe(subredditName: String): Result<Unit>
    suspend fun unsubscribe(subredditName: String): Result<Unit>
    suspend fun toggleFavorite(subredditName: String, isFavorite: Boolean)
}
