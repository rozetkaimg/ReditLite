package com.rozetka.reditlite.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.rozetka.domain.repository.UserRepository
import com.rozetka.reditlite.MainActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UnreadMessagesWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val userRepository: UserRepository by inject()

    override suspend fun doWork(): ListenableWorker.Result {
        // Проверяем статус авторизации перед запросом
        if (userRepository.getAuthStatus() !is com.rozetka.domain.model.AuthState.Authenticated) {
            return ListenableWorker.Result.success()
        }

        val result = userRepository.getUnreadMessages()
        
        return if (result.isSuccess) {
            val messages = result.getOrNull()
            if (messages != null && messages.isNotEmpty()) {
                showNotification(messages.size)
            }
            ListenableWorker.Result.success()
        } else {
            val error = result.exceptionOrNull()
            // Если ошибка 401 (Unauthorized), не ретраим, так как нужно перелогиниться
            if (error is io.ktor.client.plugins.ClientRequestException && error.response.status.value == 401) {
                ListenableWorker.Result.success()
            } else {
                ListenableWorker.Result.retry()
            }
        }
    }

    private fun showNotification(count: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "unread_messages_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(com.rozetka.reditlite.R.string.unread_messages_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(applicationContext.getString(com.rozetka.reditlite.R.string.unread_messages_notification_title))
            .setContentText(applicationContext.getString(com.rozetka.reditlite.R.string.unread_messages_notification_text, count))
            .setSmallIcon(com.rozetka.reditlite.R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
