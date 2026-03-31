package com.rozetka.presentation.ui.screen.postDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import com.rozetka.domain.usecase.post.GetCommentsUseCase
import com.rozetka.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PostDetailState {
    object Loading : PostDetailState
    data class Content(
        val post: Post,
        val comments: List<Comment>,
        val collapsedIds: Set<String> = emptySet()
    ) : PostDetailState
    data class Error(val message: String) : PostDetailState
}

class PostDetailViewModel(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _state = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val state: StateFlow<PostDetailState> = _state.asStateFlow()

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            _state.value = PostDetailState.Loading
            postRepository.getPostAndComments(postId).fold(
                onSuccess = { (post, comments) ->
                    _state.value = PostDetailState.Content(post, comments)
                },
                onFailure = { error ->
                    _state.value = PostDetailState.Error(error.message ?: "Failed to load post")
                }
            )
        }
    }

    fun toggleCommentCollapse(commentId: String) {
        _state.update { currentState ->
            if (currentState is PostDetailState.Content) {
                val newCollapsedIds = if (currentState.collapsedIds.contains(commentId)) {
                    currentState.collapsedIds - commentId
                } else {
                    currentState.collapsedIds + commentId
                }
                currentState.copy(collapsedIds = newCollapsedIds)
            } else {
                currentState
            }
        }
    }

    fun votePost(direction: VoteDirection) {
        val currentState = _state.value as? PostDetailState.Content ?: return
        viewModelScope.launch {
            postRepository.vote(currentState.post.id, direction).onSuccess {
                _state.update { 
                    if (it is PostDetailState.Content) {
                        it.copy(post = it.post.copy(voteStatus = direction))
                    } else it
                }
            }
        }
    }

    fun toggleSavePost() {
        val currentState = _state.value as? PostDetailState.Content ?: return
        viewModelScope.launch {
            val result = if (currentState.post.isSaved) {
                postRepository.unsavePost(currentState.post.id)
            } else {
                postRepository.savePost(currentState.post.id)
            }
            result.onSuccess {
                _state.update {
                    if (it is PostDetailState.Content) {
                        it.copy(post = it.post.copy(isSaved = !it.post.isSaved))
                    } else it
                }
            }
        }
    }

    fun voteComment(commentId: String, direction: VoteDirection) {
        viewModelScope.launch {
            postRepository.vote(commentId, direction).onSuccess {
                _state.update { currentState ->
                    if (currentState is PostDetailState.Content) {
                        val updatedComments = updateCommentInList(currentState.comments, commentId) { comment ->
                            val scoreDiff = when {
                                comment.voteStatus == direction -> 0
                                comment.voteStatus == VoteDirection.NONE -> direction.value
                                else -> direction.value * 2
                            }
                            comment.copy(
                                voteStatus = direction,
                                score = comment.score + scoreDiff
                            )
                        }
                        currentState.copy(comments = updatedComments)
                    } else currentState
                }
            }
        }
    }

    private fun updateCommentInList(
        comments: List<Comment>,
        commentId: String,
        transform: (Comment) -> Comment
    ): List<Comment> {
        return comments.map { comment ->
            if (comment.id == commentId) {
                transform(comment)
            } else {
                comment.copy(replies = updateCommentInList(comment.replies, commentId, transform))
            }
        }
    }

    fun submitComment(parentId: String, text: String) {
        viewModelScope.launch {
            postRepository.submitComment(parentId, text).onSuccess { newComment ->
                _state.update { currentState ->
                    if (currentState is PostDetailState.Content) {
                        // If parent is the post, add to top level
                        if (parentId == currentState.post.id) {
                            currentState.copy(comments = listOf(newComment) + currentState.comments)
                        } else {
                            // Find parent comment and add reply
                            val updatedComments = addReplyToComments(currentState.comments, parentId, newComment)
                            currentState.copy(comments = updatedComments)
                        }
                    } else currentState
                }
            }
        }
    }

    private fun addReplyToComments(comments: List<Comment>, parentId: String, newComment: Comment): List<Comment> {
        return comments.map { comment ->
            if (comment.id == parentId) {
                comment.copy(replies = listOf(newComment) + comment.replies)
            } else if (comment.replies.isNotEmpty()) {
                comment.copy(replies = addReplyToComments(comment.replies, parentId, newComment))
            } else {
                comment
            }
        }
    }
}
