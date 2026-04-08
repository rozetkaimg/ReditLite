package com.rozetka.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subreddits")
data class SubredditEntity(
    @PrimaryKey val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val iconUrl: String? = null,
    val subscribersCount: Int,
    val isSubscribed: Boolean,
    val isFavorite: Boolean = false
)
