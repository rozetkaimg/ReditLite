package com.rozetka.domain.repository

import com.rozetka.domain.model.AuthState
import com.rozetka.domain.model.NotificationMessage
import com.rozetka.domain.model.UserProfile

interface UserRepository {
    suspend fun getMyProfile(): Result<UserProfile>
    suspend fun getUnreadMessages(): Result<List<NotificationMessage>>
    suspend fun authenticate(code: String): Result<Unit>
    suspend fun getAuthStatus(): AuthState
    suspend fun setOnboardingCompleted()
    suspend fun logout()
}
