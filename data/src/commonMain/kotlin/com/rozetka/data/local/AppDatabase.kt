package com.rozetka.data.local

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.rozetka.data.model.local.PostEntity
import com.rozetka.data.model.local.SubredditEntity
import com.rozetka.data.model.local.TrophyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM reddit_posts WHERE feedType = :feedType ORDER BY orderIndex ASC")
    fun getPostsByFeedType(feedType: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM reddit_posts WHERE feedType = :feedType AND isSaved = 0")
    suspend fun clearFeed(feedType: String)

    @Query("DELETE FROM reddit_posts WHERE isSaved = 0")
    suspend fun clearAll()

    @Query("SELECT * FROM reddit_posts WHERE isSaved = 1")
    fun getSavedPosts(): Flow<List<PostEntity>>

    @Query("UPDATE reddit_posts SET isSaved = :isSaved WHERE id = :postId")
    suspend fun updateSaveStatus(postId: String, isSaved: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Query("SELECT MAX(orderIndex) FROM reddit_posts WHERE feedType = :feedType")
    suspend fun getMaxOrderIndex(feedType: String): Int?
}

@Database(
    entities = [
        PostEntity::class,
        UserEntity::class,
        TrophyEntity::class,
        SubredditEntity::class
    ],
    version = 47,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun subredditDao(): SubredditDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
