package com.rozetka.domain.usecase.auth

import com.rozetka.domain.model.AuthState
import com.rozetka.domain.repository.AuthRepository
import com.rozetka.domain.repository.UserRepository

class GetAuthUrlUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): String {
        return authRepository.getAuthorizationUrl()
    }
}
class AuthenticateUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(code: String): Result<Unit> = repository.authenticate(code)
}

class GetAuthStatusUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): AuthState = repository.getAuthStatus()
}

class SetOnboardingCompletedUseCase(private val repository: UserRepository) {
    suspend operator fun invoke() = repository.setOnboardingCompleted()
}