package com.rozetka.domain.usecase.post

import com.rozetka.domain.model.Comment
import com.rozetka.domain.repository.PostRepository

class GetCommentsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String): Result<List<Comment>> {
        return postRepository.getComments(postId)
    }
}
