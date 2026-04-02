package com.rozetka.domain.repository

interface AuthRepository {
    suspend fun getAuthorizationUrl(): String
    suspend fun exchangeCodeForToken(code: String): Result<Unit>
    suspend fun refreshToken(): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun isAuthorized(): Boolean
}
