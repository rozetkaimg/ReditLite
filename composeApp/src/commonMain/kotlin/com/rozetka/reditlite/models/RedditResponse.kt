package com.rozetka.reditlite.models



import kotlinx.serialization.Serializable

@Serializable
data class RedditResponse(val data: RedditData)
@Serializable
data class RedditData(
    val children: List<RedditPostWrapper>,
    val after: String? = null
)
@Serializable
data class RedditPostWrapper(val data: RedditPost)

@Serializable
data class RedditPost(
    val id: String,
    val name: String,
    val title: String,
    val author: String,
    val score: Int,
    val num_comments: Int = 0,
    val likes: Boolean? = null,
    val url: String? = null,
    val post_hint: String? = null
)