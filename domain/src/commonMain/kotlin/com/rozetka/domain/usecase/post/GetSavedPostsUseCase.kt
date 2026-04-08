package com.rozetka.domain.usecase.post

import com.rozetka.domain.model.Post
import com.rozetka.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow

class GetSavedPostsUseCase(
    private val repository: PostRepository
) {
    operator fun invoke(): Flow<List<Post>> {
        return repository.getLocalSavedPosts()
    }
}
