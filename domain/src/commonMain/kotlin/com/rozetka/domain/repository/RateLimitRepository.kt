package com.rozetka.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface RateLimitRepository {
    val isRateLimited: StateFlow<Boolean>
}
