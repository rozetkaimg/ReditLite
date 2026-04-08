package com.rozetka.data.remote

import kotlinx.datetime.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimitManager {
    private val mutex = Mutex()
    private var remaining: Float = 600f
    private var resetSeconds: Int = 600
    private var lastUpdateTimestamp: Long = 0

    private val _isRateLimited = MutableStateFlow(false)
    val isRateLimited: StateFlow<Boolean> = _isRateLimited.asStateFlow()

    suspend fun update(remaining: Float?, reset: Int?) {
        mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            if (remaining != null) {
                this.remaining = remaining
            }
            if (reset != null) {
                this.resetSeconds = reset
            }
            this.lastUpdateTimestamp = now
            
            updateState()
        }
    }

    private fun updateState() {
        // Если осталось меньше 10 запросов, считаем, что мы близки к лимиту
        _isRateLimited.value = remaining < 10
    }

    suspend fun checkAndDelay() {
        val (delayMs, shouldDelay) = mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val secondsPassed = (now - lastUpdateTimestamp) / 1000
            
            // Если время сброса прошло, считаем, что лимиты восстановились
            if (secondsPassed > resetSeconds) {
                remaining = 600f
                _isRateLimited.value = false
                return@withLock 0L to false
            }

            if (remaining < 10) {
                val waitTime = if (remaining <= 0) {
                    (resetSeconds - secondsPassed).coerceAtLeast(1) * 1000L
                } else {
                    1000L // Небольшая задержка, если лимиты на исходе
                }
                waitTime to true
            } else {
                0L to false
            }
        }

        if (shouldDelay) {
            delay(delayMs)
        }
    }

    suspend fun handle429() {
        mutex.withLock {
            remaining = 0f
            _isRateLimited.value = true
        }
        checkAndDelay()
    }
    
    suspend fun getRemaining(): Float = mutex.withLock { remaining }
    suspend fun getReset(): Int = mutex.withLock { resetSeconds }
}
