package com.rozetka.data.repository

import com.rozetka.data.remote.RateLimitManager
import com.rozetka.domain.repository.RateLimitRepository
import kotlinx.coroutines.flow.StateFlow

class RateLimitRepositoryImpl(
    private val rateLimitManager: RateLimitManager
) : RateLimitRepository {
    override val isRateLimited: StateFlow<Boolean> = rateLimitManager.isRateLimited
}
