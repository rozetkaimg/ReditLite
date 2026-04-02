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
data class RedditPostWrapper(
    val kind: String? = null,
    val data: NetworkPost
)

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
    val display_name: String? = null,
    val is_video: Boolean = false,
    val media: RedditMedia? = null,
    val post_hint: String? = null,
    val thumbnail: String? = null
)

@Serializable
data class RedditMedia(
    val reddit_video: RedditVideo? = null
)

@Serializable
data class RedditVideo(
    val fallback_url: String? = null,
    val dash_url: String? = null,
    val hls_url: String? = null
)

@Serializable
data class CommentListing(val data: CommentData)

@Serializable
data class CommentData(val children: List<CommentWrapper>)

@Serializable
data class CommentWrapper(
    val kind: String? = null,
    val data: NetworkComment
)

@Serializable
data class NetworkComment(
    val id: String? = null,
    val author: String? = null,
    val body: String? = null,
    val score: Int? = null,
    val created_utc: Double? = null,
    val author_icon_img: String? = null,
    val replies: JsonElement? = null,
    val depth: Int? = null
)

@Serializable
data class RedditUserResponse(
    val name: String = "",
    val link_karma: Int = 0,
    val comment_karma: Int = 0,
    val icon_img: String? = null
)

@Serializable
data class SubredditAboutResponse(
    val data: NetworkPost
)

@Serializable
data class SubredditRulesResponse(
    val rules: List<SubredditRule>
)

@Serializable
data class SubredditRule(
    val short_name: String,
    val description: String? = null,
    val violation_reason: String? = null,
    val created_utc: Double? = null,
    val priority: Int? = null,
    val description_html: String? = null
)

@Serializable
data class MediaAssetResponse(
    val args: MediaAssetArgs? = null,
    val asset: MediaAsset? = null
)

@Serializable
data class MediaAssetArgs(
    val action: String,
    val fields: List<MediaAssetField>
)

@Serializable
data class MediaAssetField(
    val name: String,
    val value: String
)

@Serializable
data class MediaAsset(
    val asset_id: String,
    val websocket_url: String? = null
)
