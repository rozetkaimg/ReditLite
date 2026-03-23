package com.rozetka.data.repository

import com.rozetka.data.local.SecureStorageManager
import com.rozetka.data.model.remote.TokenResponse
import com.rozetka.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import io.ktor.util.encodeBase64

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val storageManager: SecureStorageManager
) : AuthRepository {
    private val clientId = "yH0aTnJEt6qUgGn835B4vg"
    private val redirectUri = "redreader://rr_oauth_redir"

    override suspend fun getAuthorizationUrl(): String {
        return "https://www.reddit.com/api/v1/authorize.compact?client_id=$clientId&response_type=code&state=auth_state&redirect_uri=$redirectUri&duration=permanent&scope=identity read vote submit subscribe history mysubreddits"
    }

    override suspend fun exchangeCodeForToken(code: String): Result<Unit> {
        return runCatching {
            val encodedAuth = "$clientId:".encodeBase64()
            val response: TokenResponse = client.submitForm(
                url = "https://www.reddit.com/api/v1/access_token",
                formParameters = parameters {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", redirectUri)
                }
            ) {
                header(HttpHeaders.Authorization, "Basic $encodedAuth")
            }.body()

            storageManager.accessToken = response.access_token
            response.refresh_token?.let { storageManager.refreshToken = it }
        }
    }

    override suspend fun refreshToken(): Result<Unit> {
        return runCatching {
            val refreshToken = storageManager.refreshToken ?: throw IllegalStateException()
            val encodedAuth = "$clientId:".encodeBase64()
            val response: TokenResponse = client.submitForm(
                url = "https://www.reddit.com/api/v1/access_token",
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                }
            ) {
                header(HttpHeaders.Authorization, "Basic $encodedAuth")
            }.body()

            storageManager.accessToken = response.access_token
            response.refresh_token?.let { storageManager.refreshToken = it }
        }
    }

    override suspend fun logout(): Result<Unit> {
        return runCatching {
            storageManager.clearTokens()
        }
    }

    override fun isAuthorized(): Boolean {
        return storageManager.accessToken != null
    }
}