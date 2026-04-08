package com.rozetka.reditlite

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rozetka.reditlite.worker.UnreadMessagesWorker
import java.util.concurrent.TimeUnit

import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import com.rozetka.reditlite.di.appModule

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MainApp)
            modules(appModule)
        }
        
        if (!com.rozetka.reditlite.BuildConfig.DEBUG) {
            Napier.base(CrashlyticsAntilog())
        }

        setupUnreadMessagesWorker()
    }

    private fun setupUnreadMessagesWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<UnreadMessagesWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "unread_messages_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}