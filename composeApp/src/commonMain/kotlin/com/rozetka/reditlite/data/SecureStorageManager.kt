package com.rozetka.reditlite.data



import com.russhwolf.settings.Settings

class SecureStorageManager(private val settings: Settings) {

    var accessToken: String?
        get() = settings.getStringOrNull("access_token")
        set(value) {
            if (value != null) settings.putString("access_token", value)
            else settings.remove("access_token")
        }

    var refreshToken: String?
        get() = settings.getStringOrNull("refresh_token")
        set(value) {
            if (value != null) settings.putString("refresh_token", value)
            else settings.remove("refresh_token")
        }

    var isFirstLaunch: Boolean
        get() = settings.getBoolean("is_first_launch", true)
        set(value) = settings.putBoolean("is_first_launch", value)

    fun clearTokens() {
        settings.remove("access_token")
        settings.remove("refresh_token")
    }
}