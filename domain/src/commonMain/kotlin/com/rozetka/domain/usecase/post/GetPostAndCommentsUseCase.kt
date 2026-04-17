package com.rozetka.domain.usecase.post

import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.Post
import com.rozetka.domain.repository.PostRepository

class GetPostAndCommentsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, sort: String = "best"): Result<Pair<Post, List<Comment>>> {
        return postRepository.getPostAndComments(postId, sort)
    }
}
