package com.rozetka.domain.usecase.submit

import com.rozetka.domain.repository.PostRepository

class SubmitLinkPostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(subreddit: String, title: String, url: String): Result<Unit> {
        if (title.isBlank() || url.isBlank()) {
            return Result.failure(IllegalArgumentException("Title and URL cannot be empty"))
        }
        return postRepository.submitLinkPost(subreddit, title, url)
    }
}
