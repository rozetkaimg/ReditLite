package com.rozetka.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val totalKarma: Int,
    val linkKarma: Int,
    val commentKarma: Int,
    val iconUrl: String,
    val createdUtc: Double,
    val trophies: List<Trophy>,
    val bio: String? = null,
    val bannerUrl: String? = null
)

data class Trophy(
    val id: String?,
    val name: String,
    val description: String?,
    val iconUrl: String
)

data class SavedPost(
    val id: String,
    val title: String,
    val author: String,
    val subreddit: String,
    val score: Int
)