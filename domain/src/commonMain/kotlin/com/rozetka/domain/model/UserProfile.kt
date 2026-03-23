package com.rozetka.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val totalKarma: Int,
    val iconUrl: String,
    val trophies: List<Trophy>
)
data class Trophy(
    val name: String,
    val description: String?,
    val iconUrl: String
)