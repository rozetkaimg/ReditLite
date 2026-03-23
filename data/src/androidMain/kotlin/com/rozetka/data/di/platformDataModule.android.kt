package com.rozetka.data.di


import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.rozetka.data.local.AppDatabase
import org.koin.dsl.module

actual val platformDataModule = module {
    single<AppDatabase> {
        val context = get<Context>()
        val dbFile = context.getDatabasePath("reddit_lite.db")
        Room.databaseBuilder<AppDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
            .setDriver(AndroidSQLiteDriver())
            .build()
    }
}