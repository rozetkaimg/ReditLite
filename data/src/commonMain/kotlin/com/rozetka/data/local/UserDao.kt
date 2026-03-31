package com.rozetka.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rozetka.data.model.local.TrophyEntity


@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    suspend fun getUserProfile(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(user: UserEntity)

    @Query("SELECT * FROM trophies")
    suspend fun getTrophies(): List<TrophyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrophies(trophies: List<TrophyEntity>)

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()

    @Query("DELETE FROM trophies")
    suspend fun clearTrophies()
}