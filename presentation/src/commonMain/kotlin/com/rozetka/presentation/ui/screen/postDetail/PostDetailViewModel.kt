package com.rozetka.presentation.ui.screen.postDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import com.rozetka.domain.usecase.post.GetPostAndCommentsUseCase
import com.rozetka.domain.usecase.post.SubmitCommentUseCase
import com.rozetka.domain.usecase.post.ToggleSavePostUseCase
import com.rozetka.domain.usecase.post.VoteUseCase
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
    private val getPostAndCommentsUseCase: GetPostAndCommentsUseCase,
    private val voteUseCase: VoteUseCase,
    private val toggleSavePostUseCase: ToggleSavePostUseCase,
    private val submitCommentUseCase: SubmitCommentUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val state: StateFlow<PostDetailState> = _state.asStateFlow()

    private val _currentSort = MutableStateFlow("best")
    val currentSort: StateFlow<String> = _currentSort.asStateFlow()

    private val _selectedMedia = MutableStateFlow<Pair<ByteArray, String>?>(null)
    val selectedMedia = _selectedMedia.asStateFlow()

    fun onMediaSelected(bytes: ByteArray, fileName: String) {
        _selectedMedia.value = bytes to fileName
    }

    fun clearMedia() {
        _selectedMedia.value = null
    }

    fun loadPostDetail(postId: String, sort: String = "best") {
        viewModelScope.launch {
            _currentSort.value = sort
            _state.value = PostDetailState.Loading
            getPostAndCommentsUseCase(postId, sort).fold(
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
        val finalDirection = if (currentState.post.voteStatus == direction) VoteDirection.NONE else direction

        viewModelScope.launch {
            voteUseCase(currentState.post.id, currentState.post.voteStatus, direction).onSuccess {
                _state.update { state ->
                    if (state is PostDetailState.Content) {
                        val scoreDiff = finalDirection.value - state.post.voteStatus.value
                        state.copy(
                            post = state.post.copy(
                                voteStatus = finalDirection,
                                score = state.post.score + scoreDiff
                            )
                        )
                    } else state
                }
            }
        }
    }

    fun toggleSavePost() {
        val currentState = _state.value as? PostDetailState.Content ?: return
        viewModelScope.launch {
            toggleSavePostUseCase(currentState.post).onSuccess {
                _state.update {
                    if (it is PostDetailState.Content) {
                        it.copy(post = it.post.copy(isSaved = !it.post.isSaved))
                    } else it
                }
            }
        }
    }

    fun voteComment(commentId: String, direction: VoteDirection) {
        val currentState = _state.value as? PostDetailState.Content ?: return

        val targetComment = findCommentById(currentState.comments, commentId) ?: return
        val finalDirection = if (targetComment.voteStatus == direction) VoteDirection.NONE else direction
        val scoreDiff = finalDirection.value - targetComment.voteStatus.value

        viewModelScope.launch {
            voteUseCase(commentId, targetComment.voteStatus, direction).onSuccess {
                _state.update { state ->
                    if (state is PostDetailState.Content) {
                        val updatedComments = updateCommentInList(state.comments, commentId) { comment ->
                            comment.copy(
                                voteStatus = finalDirection,
                                score = comment.score + scoreDiff
                            )
                        }
                        state.copy(comments = updatedComments)
                    } else state
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

    private fun findCommentById(comments: List<Comment>, commentId: String): Comment? {
        comments.forEach { comment ->
            if (comment.id == commentId) return comment
            findCommentById(comment.replies, commentId)?.let { return it }
        }
        return null
    }

    fun submitComment(parentId: String, text: String) {
        viewModelScope.launch {
            val media = _selectedMedia.value
            submitCommentUseCase(parentId, text, media?.first, media?.second).onSuccess { newComment ->
                clearMedia()
                _state.update { currentState ->
                    if (currentState is PostDetailState.Content) {
                        if (parentId == currentState.post.id) {
                            currentState.copy(comments = listOf(newComment) + currentState.comments)
                        } else {
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
