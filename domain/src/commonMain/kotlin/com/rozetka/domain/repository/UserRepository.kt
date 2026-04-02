package com.rozetka.domain.repository

import com.rozetka.domain.model.AuthState
import com.rozetka.domain.model.NotificationMessage
import com.rozetka.domain.model.SavedPost
import com.rozetka.domain.model.Trophy
import com.rozetka.domain.model.UserProfile

interface UserRepository {
    suspend fun getUnreadMessages(): Result<List<NotificationMessage>>
    suspend fun authenticate(code: String): Result<Unit>
    suspend fun getAuthStatus(): AuthState
    suspend fun setOnboardingCompleted()
    suspend fun logout()
    suspend fun getMyProfile(): Result<UserProfile>
    suspend fun getTrophies(): Result<List<Trophy>>
    suspend fun getSavedPosts(username: String): Result<List<SavedPost>>
}

