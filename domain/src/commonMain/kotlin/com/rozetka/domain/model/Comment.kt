package com.rozetka.domain.model

data class Comment(
    val id: String,
    val author: String,
    val body: String,
    val score: Int,
    val voteStatus: VoteDirection,
    val depth: Int,
    val replies: List<Comment>,
    val authorIconUrl: String? = null,
    val mediaUrls: List<String> = emptyList()
)