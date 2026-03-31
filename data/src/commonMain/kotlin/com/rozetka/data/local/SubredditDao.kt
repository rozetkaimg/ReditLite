package com.rozetka.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rozetka.data.model.local.SubredditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubredditDao {
    @Query("SELECT * FROM subreddits")
    fun getSubreddits(): Flow<List<SubredditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubreddits(subreddits: List<SubredditEntity>)

    @Query("DELETE FROM subreddits")
    suspend fun clearSubreddits()

    @Query("UPDATE subreddits SET isSubscribed = :isSubscribed WHERE name = :subredditName")
    suspend fun updateSubscriptionStatus(subredditName: String, isSubscribed: Boolean)

    @Query("UPDATE subreddits SET isFavorite = :isFavorite WHERE name = :subredditName")
    suspend fun updateFavoriteStatus(subredditName: String, isFavorite: Boolean)

    @Query("SELECT * FROM subreddits WHERE isFavorite = 1")
    fun getFavoriteSubreddits(): Flow<List<SubredditEntity>>
}
