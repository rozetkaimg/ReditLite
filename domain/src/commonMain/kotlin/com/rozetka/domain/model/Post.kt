package com.rozetka.domain.model

import com.rozetka.domain.model.VoteDirection

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
    val galleryUrls: List<String>? = null,
    val isVideo: Boolean = false,
    val videoUrl: String? = null,
    val postUrl: String,
    val createdUtc: Long
)