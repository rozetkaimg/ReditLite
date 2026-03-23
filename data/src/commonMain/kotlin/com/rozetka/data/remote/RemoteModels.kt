package com.rozetka.data.model.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val scope: String,
    val refresh_token: String? = null
)

@Serializable
data class RedditListingResponse(val data: RedditListingData)

@Serializable
data class RedditListingData(
    val children: List<RedditPostWrapper>,
    val after: String? = null,
    val before: String? = null
)

@Serializable
data class RedditPostWrapper(val data: NetworkPost)

@Serializable
data class NetworkPost(
    val name: String,
    val id: String = "",
    val title: String? = null,
    val author: String? = null,
    val subreddit: String? = null,
    val score: Int = 0,
    val num_comments: Int = 0,
    val saved: Boolean = false,
    val likes: Boolean? = null,
    val selftext: String? = null,
    val body: String? = null,
    val url: String? = null,
    val icon_img: String? = null,
    val public_description: String? = null,
    val subscribers: Int = 0,
    val subject: String? = null,
    val created_utc: Double = 0.0,
    val display_name: String? = null
)

@Serializable
data class CommentListing(val data: CommentData)

@Serializable
data class CommentData(val children: List<CommentWrapper>)

@Serializable
data class CommentWrapper(val data: NetworkComment)

@Serializable
data class NetworkComment(
    val id: String? = null,
    val author: String? = null,
    val body: String? = null,
    val score: Int? = null,
    val created_utc: Double? = null,
    val replies: JsonElement? = null
)

@Serializable
data class RedditUserResponse(
    val name: String = "",
    val link_karma: Int = 0,
    val comment_karma: Int = 0,
    val icon_img: String? = null
)