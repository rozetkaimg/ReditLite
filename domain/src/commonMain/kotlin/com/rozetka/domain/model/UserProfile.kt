package com.rozetka.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val totalKarma: Int,
    val iconUrl: String,
    val trophies: List<Trophy>
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