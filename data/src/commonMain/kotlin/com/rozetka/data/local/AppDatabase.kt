package com.rozetka.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.rozetka.data.model.local.PostEntity
import com.rozetka.data.model.local.SubredditEntity
import com.rozetka.data.model.local.TrophyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM reddit_posts WHERE feedType = :feedType ORDER BY createdUtc DESC")
    fun getPostsByFeedType(feedType: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM reddit_posts WHERE feedType = :feedType")
    suspend fun clearFeed(feedType: String)

    @Query("SELECT * FROM reddit_posts WHERE isSaved = 1")
    fun getSavedPosts(): Flow<List<PostEntity>>

    @Query("UPDATE reddit_posts SET isSaved = :isSaved WHERE id = :postId")
    suspend fun updateSaveStatus(postId: String, isSaved: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Query("DELETE FROM reddit_posts")
    suspend fun clearAll()
}

@Database(
    entities = [
        PostEntity::class,
        UserEntity::class,
        TrophyEntity::class,
        SubredditEntity::class
    ],
    version = 45,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun subredditDao(): SubredditDao
}