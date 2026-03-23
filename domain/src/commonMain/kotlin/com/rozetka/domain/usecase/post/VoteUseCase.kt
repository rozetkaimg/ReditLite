package com.rozetka.domain.usecase.post

import com.rozetka.domain.model.VoteDirection
import com.rozetka.domain.repository.PostRepository

class VoteUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, currentDirection: VoteDirection, newDirection: VoteDirection): Result<Unit> {
        val finalDirection = if (currentDirection == newDirection) VoteDirection.NONE else newDirection
        return postRepository.vote(postId, finalDirection)
    }
}
