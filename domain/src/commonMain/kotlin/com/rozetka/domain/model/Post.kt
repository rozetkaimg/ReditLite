package com.rozetka.domain.model

data class Post(
    val id: String,
    val title: String,
    val author: String,
    val subreddit: String,
    val score: Int,
    val commentsCount: Int,
    val isSaved: Boolean,
    val voteStatus: VoteDirection,
    val text: String?,
    val mediaUrl: String?,
    val isVideo: Boolean = false,
    val videoUrl: String? = null,
    val postUrl: String,
    val createdUtc: Long
)