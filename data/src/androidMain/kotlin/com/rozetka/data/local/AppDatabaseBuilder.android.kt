package com.rozetka.data.local



import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver

actual class AppDatabaseBuilder(private val context: Context) {
    actual fun build(): AppDatabase {
        val dbFile = context.getDatabasePath("reddit_lite.db")
        return Room.databaseBuilder<AppDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
            .setDriver(AndroidSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    }
}