package com.rozetka.domain.usecase.post

import com.rozetka.domain.model.Comment
import com.rozetka.domain.repository.PostRepository

class SubmitCommentUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(
        parentId: String,
        text: String,
        mediaBytes: ByteArray? = null,
        fileName: String? = null
    ): Result<Comment> {
        return postRepository.submitComment(parentId, text, mediaBytes, fileName)
    }
}
