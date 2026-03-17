package com.rozetka.reditlite.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reddit_posts")
data class RedditPostEntity(
    @PrimaryKey val id: String,
    val name: String,
    val title: String,
    val author: String,
    val score: Int,
    val numComments: Int,
    val likes: Boolean?,
    val url: String?,
    val postHint: String?
)

@Dao
interface RedditPostDao {
    @Query("SELECT * FROM reddit_posts")
    fun getAllPosts(): Flow<List<RedditPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<RedditPostEntity>)

    @Query("DELETE FROM reddit_posts")
    suspend fun clearAll()
}

@Database(entities = [RedditPostEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun redditPostDao(): RedditPostDao
}