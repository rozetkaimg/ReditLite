package com.rozetka.domain.repository

import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun getComments(postId: String): Result<List<Comment>>
    suspend fun getPostAndComments(postId: String, sort: String = "best"): Result<Pair<Post, List<Comment>>>
    suspend fun vote(id: String, direction: VoteDirection): Result<Unit>
    suspend fun savePost(postId: String): Result<Unit>
    suspend fun unsavePost(postId: String): Result<Unit>
    suspend fun savePostLocally(post: Post)
    fun getLocalSavedPosts(): Flow<List<Post>>
    suspend fun submitTextPost(subreddit: String, title: String, text: String): Result<Unit>
    suspend fun submitLinkPost(subreddit: String, title: String, url: String): Result<Unit>
    suspend fun submitImagePost(subreddit: String, title: String, imageUri: String): Result<Unit>
    suspend fun submitComment(parentId: String, text: String): Result<Comment>
    fun enqueuePostSubmission(subreddit: String, title: String, content: String, type: String, mediaUri: String? = null)
}
