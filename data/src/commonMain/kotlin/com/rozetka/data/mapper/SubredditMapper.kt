package com.rozetka.data.mapper

import com.rozetka.data.model.local.SubredditEntity
import com.rozetka.data.model.remote.NetworkPost
import com.rozetka.domain.model.Subreddit

fun NetworkPost.toSubredditEntity(): SubredditEntity = SubredditEntity(
    id = this.name,
    name = this.display_name ?: this.subreddit ?: this.name,
    displayName = this.display_name ?: this.subreddit ?: this.name,
    description = this.public_description ?: "",
    iconUrl = this.icon_img,
    subscribersCount = this.subscribers,
    isSubscribed = true, // Since we fetch from /subreddits/mine/subscriber
    isFavorite = false
)

fun SubredditEntity.toDomain(): Subreddit = Subreddit(
    id = this.id,
    name = this.name,
    displayName = this.displayName,
    description = this.description,
    iconUrl = this.iconUrl,
    subscribersCount = this.subscribersCount,
    isSubscribed = this.isSubscribed,
    isFavorite = this.isFavorite
)
