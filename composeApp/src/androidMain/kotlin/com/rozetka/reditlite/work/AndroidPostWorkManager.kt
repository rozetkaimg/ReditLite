package com.rozetka.reditlite.work

import android.content.Context
import androidx.work.*
import com.rozetka.domain.work.PostWorkManager
import com.rozetka.reditlite.worker.PostSubmitWorker

class AndroidPostWorkManager(private val context: Context) : PostWorkManager {
    override fun enqueuePostSubmission(
        subreddit: String,
        title: String,
        content: String,
        type: String,
        mediaUri: String?
    ) {
        val data = workDataOf(
            "subreddit" to subreddit,
            "title" to title,
            "content" to content,
            "type" to type,
            "mediaUri" to mediaUri
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<PostSubmitWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, java.util.concurrent.TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
