package com.rozetka.domain.model

data class Subreddit(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val iconUrl: String? = null,
    val subscribersCount: Int,
    val isSubscribed: Boolean,
    val isFavorite: Boolean = false,
    val rules: List<String> = emptyList()
)
