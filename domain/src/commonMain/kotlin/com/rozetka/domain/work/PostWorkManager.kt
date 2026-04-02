package com.rozetka.domain.work

interface PostWorkManager {
    fun enqueuePostSubmission(
        subreddit: String,
        title: String,
        content: String,
        type: String,
        mediaUri: String? = null
    )
}
