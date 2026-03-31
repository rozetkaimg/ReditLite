package com.rozetka.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reddit_posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val subreddit: String,
    val score: Int,
    val commentsCount: Int,
    val isSaved: Boolean,
    val voteStatus: Int,
    val text: String?,
    val mediaUrl: String?,
    val isVideo: Boolean = false,
    val videoUrl: String? = null,
    val postUrl: String,
    val createdUtc: Long,
    val feedType: String
)