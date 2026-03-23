package com.rozetka.domain.model

data class Subreddit(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val subscribersCount: Int,
    val isSubscribed: Boolean
)
data class Comment(
    val id: String,
    val author: String,
    val body: String,
    val score: Int,
    val voteStatus: VoteDirection,
    val depth: Int,
    val replies: List<Comment>
)