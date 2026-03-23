package com.rozetka.domain.usecase.subreddit

import com.rozetka.domain.model.UserProfile
import com.rozetka.domain.repository.UserRepository

class GetProfileUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): Result<UserProfile> {
        return repository.getMyProfile()
    }
}