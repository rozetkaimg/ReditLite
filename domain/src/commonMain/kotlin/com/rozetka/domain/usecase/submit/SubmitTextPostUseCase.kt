package com.rozetka.domain.usecase.submit

import com.rozetka.domain.repository.PostRepository

class SubmitTextPostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(subreddit: String, title: String, text: String): Result<Unit> {
        if (title.isBlank() || text.isBlank()) {
            return Result.failure(IllegalArgumentException("Title and text cannot be empty"))
        }
        return postRepository.submitTextPost(subreddit, title, text)
    }
}
