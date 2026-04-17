package com.rozetka.domain.usecase.post

import com.rozetka.domain.model.Post
import com.rozetka.domain.repository.PostRepository

class SearchPostsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(subreddit: String, query: String): Result<List<Post>> {
        if (query.isBlank()) return Result.success(emptyList())
        return postRepository.searchPosts(subreddit, query)
    }
}
