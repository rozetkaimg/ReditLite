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
    postUrl = this.postUrl,
    createdUtc = this.createdUtc
)

fun NetworkPost.toEntity(feedType: String): PostEntity = PostEntity(
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
    mediaUrl = this.url,
    postUrl = "https://reddit.com${this.url}",
    createdUtc = this.created_utc.toLong(),
    feedType = feedType
)

fun NetworkPost.toDomain(): Post = Post(
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
    mediaUrl = this.url,
    postUrl = "https://reddit.com${this.url}",
    createdUtc = this.created_utc.toLong()
)

fun RedditUserResponse.toDomain(): UserProfile = UserProfile(
    id = this.name,
    name = this.name,
    totalKarma = this.link_karma + this.comment_karma,
    iconUrl = this.icon_img ?: "",
    trophies = emptyList()
)

fun NetworkPost.toSubredditDomain(): Subreddit = Subreddit(
    id = this.name,
    name = this.display_name ?: this.subreddit ?: this.name,
    displayName = this.display_name ?: this.subreddit ?: this.name,
    description = this.public_description ?: "",
    subscribersCount = this.subscribers,
    isSubscribed = false
)

fun NetworkComment.toDomain(depth: Int = 0): Comment {
    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val parsedReplies = try {
        if (this.replies != null && this.replies.jsonObject.isNotEmpty()) {
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

    return Comment(
        id = this.id ?: "",
        author = this.author ?: "[deleted]",
        body = this.body ?: "",
        score = this.score ?: 0,
        voteStatus = VoteDirection.NONE,
        depth = depth,
        replies = parsedReplies
    )
}