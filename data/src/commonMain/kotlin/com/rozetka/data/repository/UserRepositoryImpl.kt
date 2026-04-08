package com.rozetka.data.repository

import com.rozetka.data.local.AppDatabase
import com.rozetka.data.local.SecureStorageManager
import com.rozetka.data.local.UserEntity
import com.rozetka.data.mapper.toDomain
import com.rozetka.data.model.local.TrophyEntity
import com.rozetka.data.model.local.RedditTrophyResponse
import com.rozetka.data.model.remote.RedditListingResponse
import com.rozetka.data.model.remote.RedditUserResponse
import com.rozetka.data.model.remote.RedditUserWrapper
import com.rozetka.domain.model.AuthState
import com.rozetka.domain.model.NotificationMessage
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.SavedPost
import com.rozetka.domain.model.Trophy
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
            try {
                val response: RedditUserResponse =
                    client.get("https://oauth.reddit.com/api/v1/me").body()
                
                val trophiesResult = getTrophies()
                val trophies = trophiesResult.getOrDefault(emptyList())

                val profile = UserProfile(
                    id = response.id,
                    name = response.name,
                    totalKarma = response.total_karma,
                    linkKarma = response.link_karma,
                    commentKarma = response.comment_karma,
                    iconUrl = response.icon_img?.substringBefore("?") ?: "",
                    createdUtc = response.created_utc,
                    trophies = trophies
                )

                database.userDao().insertUserProfile(
                    UserEntity(
                        name = profile.name,
                        totalKarma = profile.totalKarma,
                        linkKarma = profile.linkKarma,
                        commentKarma = profile.commentKarma,
                        iconUrl = profile.iconUrl,
                        createdUtc = profile.createdUtc
                    )
                )
                profile
            } catch (e: Exception) {
                val cachedProfile = database.userDao().getUserProfile()
                if (cachedProfile != null) {
                    cachedProfile.toDomain()
                } else throw e
            }
        }
    }

    override suspend fun getUnreadMessages(): Result<List<NotificationMessage>> {
        return runCatching {
            val response: RedditListingResponse =
                client.get("https://oauth.reddit.com/message/unread").body()
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
        database.userDao().clearProfile()
        database.userDao().clearTrophies()
    }

    override suspend fun getTrophies(): Result<List<Trophy>> {
        return runCatching {
            try {
                val response: RedditTrophyResponse =
                    client.get("https://oauth.reddit.com/api/v1/me/trophies").body()
                val trophies = response.data.trophies.map {
                    Trophy(
                        id = it.data.id ?: "",
                        name = it.data.name,
                        iconUrl = it.data.icon_70 ?: "",
                        description = it.data.description
                    )
                }

                database.userDao().clearTrophies()
                database.userDao().insertTrophies(
                    trophies.map { TrophyEntity(it.id?: "",  it.name, it.iconUrl, it.description) }
                )
                trophies
            } catch (e: Exception) {
                val cachedTrophies = database.userDao().getTrophies()
                if (cachedTrophies.isNotEmpty()) {
                    cachedTrophies.map { it.toDomain() }
                } else {
                    throw e
                }
            }
        }
    }

    override suspend fun getSavedPosts(username: String): Result<List<Post>> {
        return runCatching {
            val response: RedditListingResponse =
                client.get("https://oauth.reddit.com/user/$username/saved").body()
            response.data.children.map { it.data.toDomain() }
        }
    }

    override suspend fun getUserPosts(username: String): Result<List<Post>> {
        return runCatching {
            val response: RedditListingResponse =
                client.get("https://oauth.reddit.com/user/$username/submitted").body()
            response.data.children.map { it.data.toDomain() }
        }
    }

    override suspend fun getUserProfile(username: String): Result<UserProfile> {
        return runCatching {
            val response: RedditUserWrapper =
                client.get("https://oauth.reddit.com/user/$username/about").body()
            val data = response.data
            UserProfile(
                id = data.id,
                name = data.name,
                totalKarma = data.total_karma,
                linkKarma = data.link_karma,
                commentKarma = data.comment_karma,
                iconUrl = data.icon_img?.substringBefore("?") ?: "",
                createdUtc = data.created_utc,
                trophies = emptyList() // Or fetch if needed
            )
        }
    }
}