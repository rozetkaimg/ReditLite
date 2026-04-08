package com.rozetka.domain.usecase.auth

import com.rozetka.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): Result<Unit> {
        return authRepository.exchangeCodeForToken(code)
    }
}
