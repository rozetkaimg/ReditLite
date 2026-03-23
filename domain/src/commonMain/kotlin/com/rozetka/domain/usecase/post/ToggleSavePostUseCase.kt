package com.rozetka.domain.usecase.post

import com.rozetka.domain.repository.PostRepository

class ToggleSavePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, isCurrentlySaved: Boolean): Result<Unit> {
        return if (isCurrentlySaved) {
            postRepository.unsavePost(postId)
        } else {
            postRepository.savePost(postId)
        }
    }
}
