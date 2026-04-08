package com.rozetka.domain.usecase.post

import com.rozetka.domain.model.Post
import com.rozetka.domain.repository.PostRepository

class ToggleSavePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(post: Post): Result<Unit> {
        return if (post.isSaved) {
            postRepository.unsavePost(post.id)
        } else {
            postRepository.savePostLocally(post)
            postRepository.savePost(post.id)
        }
    }
}
