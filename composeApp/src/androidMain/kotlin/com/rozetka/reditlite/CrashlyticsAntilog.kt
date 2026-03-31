package com.rozetka.reditlite

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel

class CrashlyticsAntilog : Antilog() {
    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        
        tag?.let { crashlytics.setCustomKey("tag", it) }
        message?.let { crashlytics.log(it) }
        
        if (priority == LogLevel.ERROR || priority == LogLevel.WARNING) {
            throwable?.let { crashlytics.recordException(it) }
        }
    }
}