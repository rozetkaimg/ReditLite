package com.rozetka.domain.usecase.auth

import com.rozetka.domain.repository.AuthRepository

class CheckAuthStatusUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean {
        return authRepository.isAuthorized()
    }
}
