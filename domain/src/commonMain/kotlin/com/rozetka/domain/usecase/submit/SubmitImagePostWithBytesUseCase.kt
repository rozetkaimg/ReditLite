package com.rozetka.domain.usecase.submit

import com.rozetka.domain.repository.PostRepository

class SubmitImagePostWithBytesUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(
        subreddit: String,
        title: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<Unit> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Title cannot be empty"))
        }
        if (fileName.isBlank()) {
            return Result.failure(IllegalArgumentException("File name cannot be empty"))
        }
        return postRepository.submitImagePostWithBytes(subreddit, title, imageBytes, fileName)
    }
}
