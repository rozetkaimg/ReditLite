package com.rozetka.data.mapper

import com.rozetka.data.model.local.PostEntity
import com.rozetka.data.model.remote.CommentListing
import com.rozetka.data.model.remote.NetworkComment
import com.rozetka.data.model.remote.NetworkPost
import com.rozetka.data.model.remote.RedditUserResponse
import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.Subreddit
import com.rozetka.domain.model.UserProfile
import com.rozetka.domain.model.VoteDirection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

fun PostEntity.toDomain(): Post = Post(
    id = this.id,
    title = this.title,
    author = this.author,
    subreddit = this.subreddit,
    score = this.score,
    commentsCount = this.commentsCount,
    isSaved = this.isSaved,
    voteStatus = when (this.voteStatus) {
        1 -> VoteDirection.UP
        -1 -> VoteDirection.DOWN
        else -> VoteDirection.NONE
    },
    text = this.text,
    mediaUrl = this.mediaUrl,
    galleryUrls = this.galleryUrls?.split(",")?.filter { it.isNotBlank() },
    isVideo = this.isVideo,
    videoUrl = this.videoUrl,
    postUrl = this.postUrl,
    createdUtc = this.createdUtc
)

fun Post.toEntity(feedType: String = "saved", orderIndex: Int = 0): PostEntity = PostEntity(
    id = this.id,
    title = this.title,
    author = this.author,
    subreddit = this.subreddit,
    score = this.score,
    commentsCount = this.commentsCount,
    isSaved = this.isSaved,
    voteStatus = when (this.voteStatus) {
        VoteDirection.UP -> 1
        VoteDirection.DOWN -> -1
        VoteDirection.NONE -> 0
    },
    text = this.text,
    mediaUrl = this.mediaUrl,
    galleryUrls = this.galleryUrls?.joinToString(","),
    isVideo = this.isVideo,
    videoUrl = this.videoUrl,
    postUrl = this.postUrl,
    createdUtc = this.createdUtc,
    feedType = feedType,
    orderIndex = orderIndex
)

fun NetworkPost.toEntity(feedType: String, orderIndex: Int = 0): PostEntity {
    val isValidMedia = (this.post_hint == "image" || this.post_hint == "rich:video" || this.post_hint == "video") ||
            (this.url?.endsWith(".jpg") == true || this.url?.endsWith(".png") == true || this.url?.endsWith(".gif") == true)
    val mediaUrl = if (isValidMedia) this.url else if (this.thumbnail?.startsWith("http") == true) this.thumbnail else null

    val galleryUrls = if (this.is_gallery && this.gallery_data != null && this.media_metadata != null) {
        this.gallery_data.items.mapNotNull { item ->
            this.media_metadata[item.media_id]?.s?.u?.replace("&amp;", "&")
        }.joinToString(",")
    } else null

    return PostEntity(
        id = this.name,
        title = this.title ?: "",
        author = this.author ?: "",
        subreddit = this.subreddit ?: "",
        score = this.score,
        commentsCount = this.num_comments,
        isSaved = this.saved,
        voteStatus = when (this.likes) {
            true -> 1
            false -> -1
            null -> 0
        },
        text = this.selftext,
        mediaUrl = mediaUrl,
        galleryUrls = galleryUrls,
        isVideo = this.is_video,
        videoUrl = this.media?.reddit_video?.hls_url?.takeIf { it.isNotBlank() }
            ?: this.media?.reddit_video?.fallback_url,
        postUrl = "https://reddit.com${this.permalink}",
        createdUtc = this.created_utc.toLong(),
        feedType = feedType,
        orderIndex = orderIndex
    )
}

fun NetworkPost.toDomain(): Post {
    val isValidMedia = (this.post_hint == "image" || this.post_hint == "rich:video" || this.post_hint == "video") ||
            (this.url?.endsWith(".jpg") == true || this.url?.endsWith(".png") == true || this.url?.endsWith(".gif") == true)
    val mediaUrl = if (isValidMedia) this.url else if (this.thumbnail?.startsWith("http") == true) this.thumbnail else null

    val galleryUrls = if (this.is_gallery && this.gallery_data != null && this.media_metadata != null) {
        this.gallery_data.items.mapNotNull { item ->
            this.media_metadata[item.media_id]?.s?.u?.replace("&amp;", "&")
        }
    } else null

    return Post(
        id = this.name,
        title = this.title ?: "",
        author = this.author ?: "",
        subreddit = this.subreddit ?: "",
        score = this.score,
        commentsCount = this.num_comments,
        isSaved = this.saved,
        voteStatus = when (this.likes) {
            true -> VoteDirection.UP
            false -> VoteDirection.DOWN
            null -> VoteDirection.NONE
        },
        text = this.selftext,
        mediaUrl = mediaUrl,
        galleryUrls = galleryUrls,
        isVideo = this.is_video,
        videoUrl = this.media?.reddit_video?.hls_url?.takeIf { it.isNotBlank() }
            ?: this.media?.reddit_video?.fallback_url,
        postUrl = "https://reddit.com${this.permalink}",
        createdUtc = this.created_utc.toLong()
    )
}

fun RedditUserResponse.toDomain(): UserProfile = UserProfile(
    id = this.id,
    name = this.name,
    totalKarma = this.total_karma,
    linkKarma = this.link_karma,
    commentKarma = this.comment_karma,
    iconUrl = this.icon_img?.substringBefore("?") ?: "",
    createdUtc = this.created_utc,
    trophies = emptyList(),
    bio = this.subreddit?.public_description,
    bannerUrl = this.subreddit?.url?.substringBefore("?")
)

fun NetworkPost.toSubredditDomain(): Subreddit = Subreddit(
    id = this.name,
    name = this.display_name ?: this.subreddit ?: this.name,
    displayName = this.display_name ?: this.subreddit ?: this.name,
    description = this.public_description ?: "",
    iconUrl = this.icon_img ?: this.thumbnail,
    subscribersCount = this.subscribers,
    isSubscribed = this.user_is_subscriber ?: false
)

fun NetworkComment.toDomain(depth: Int = 0): Comment {
    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val parsedReplies = try {
        if (this.replies != null && this.replies.toString().contains("data")) {
            val listing = json.decodeFromJsonElement<CommentListing>(this.replies)
            listing.data.children.mapNotNull {
                it.data.toDomain(depth + 1)
            }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }

    val extractedMediaUrls = this.media_metadata?.values?.mapNotNull { item ->
        val source = item.s
        (source?.gif ?: source?.u ?: source?.mp4)?.replace("&amp;", "&")
    } ?: emptyList()

    return Comment(
        id = this.id ?: "",
        author = this.author ?: "[deleted]",
        body = this.body ?: "",
        score = this.score ?: 0,
        voteStatus = VoteDirection.NONE,
        depth = depth,
        replies = parsedReplies,
        authorIconUrl = this.author_icon_img,
        mediaUrls = extractedMediaUrls
    )
}