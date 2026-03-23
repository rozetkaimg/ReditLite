package com.rozetka.data.repository

import com.rozetka.data.local.AppDatabase
import com.rozetka.data.local.SecureStorageManager
import com.rozetka.data.mapper.toDomain
import com.rozetka.data.model.remote.RedditListingResponse
import com.rozetka.data.model.remote.RedditUserResponse
import com.rozetka.domain.model.AuthState
import com.rozetka.domain.model.NotificationMessage
import com.rozetka.domain.model.UserProfile
import com.rozetka.domain.repository.AuthRepository
import com.rozetka.domain.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UserRepositoryImpl(
    private val client: HttpClient,
    private val storageManager: SecureStorageManager,
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) : UserRepository {

    override suspend fun getMyProfile(): Result<UserProfile> {
        return runCatching {
            val response: RedditUserResponse = client.get("https://oauth.reddit.com/api/v1/me").body()
            response.toDomain()
        }
    }

    override suspend fun getUnreadMessages(): Result<List<NotificationMessage>> {
        return runCatching {
            val response: RedditListingResponse = client.get("https://oauth.reddit.com/message/unread").body()
            response.data.children.map { wrapper ->
                NotificationMessage(
                    id = wrapper.data.name,
                    author = wrapper.data.author ?: "",
                    subject = wrapper.data.subject ?: "",
                    body = wrapper.data.body ?: "",
                    isUnread = true
                )
            }
        }
    }

    override suspend fun authenticate(code: String): Result<Unit> {
        return authRepository.exchangeCodeForToken(code)
    }

    override suspend fun getAuthStatus(): AuthState {
        return if (!storageManager.isFirstLaunch && storageManager.accessToken != null) {
            AuthState.Authenticated
        } else {
            AuthState.Initial
        }
    }

    override suspend fun setOnboardingCompleted() {
        storageManager.isFirstLaunch = false
    }

    override suspend fun logout() {
        storageManager.clearTokens()
        database.postDao().clearAll()
    }
}