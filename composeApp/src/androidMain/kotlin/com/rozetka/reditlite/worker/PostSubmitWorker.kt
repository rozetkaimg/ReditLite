package com.rozetka.reditlite.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.rozetka.domain.repository.PostRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PostSubmitWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val postRepository: PostRepository by inject()

    override suspend fun doWork(): ListenableWorker.Result {
        val subreddit = inputData.getString("subreddit") ?: return ListenableWorker.Result.failure()
        val title = inputData.getString("title") ?: return ListenableWorker.Result.failure()
        val content = inputData.getString("content") ?: ""
        val type = inputData.getString("type") ?: "TEXT"
        val mediaUri = inputData.getString("mediaUri")

        val result: kotlin.Result<Unit> = when (type) {
            "TEXT" -> postRepository.submitTextPost(subreddit, title, content)
            "LINK" -> postRepository.submitLinkPost(subreddit, title, content)
            "IMAGE" -> {
                mediaUri?.let {
                    postRepository.submitImagePost(subreddit, title, it)
                } ?: kotlin.Result.failure(Exception("No media URI"))
            }
            else -> kotlin.Result.failure(Exception("Unknown post type"))
        }

        return if (result.isSuccess) {
            ListenableWorker.Result.success()
        } else {
            ListenableWorker.Result.retry()
        }
    }
}
