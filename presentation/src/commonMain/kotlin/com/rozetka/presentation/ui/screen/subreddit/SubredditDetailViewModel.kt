package com.rozetka.presentation.ui.screen.subreddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.VoteDirection
import com.rozetka.domain.usecase.feed.FetchSubredditFeedUseCase
import com.rozetka.domain.usecase.post.SearchPostsUseCase
import com.rozetka.domain.usecase.post.ToggleSavePostUseCase
import com.rozetka.domain.usecase.post.VoteUseCase
import com.rozetka.domain.usecase.subreddit.GetSubredditInfoUseCase
import com.rozetka.domain.usecase.subreddit.GetSubredditRulesUseCase
import com.rozetka.domain.usecase.subreddit.ToggleSubscriptionUseCase
import com.rozetka.presentation.mvi.SubredditDetailIntent
import com.rozetka.presentation.mvi.SubredditDetailState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubredditDetailViewModel(
    private val getSubredditInfoUseCase: GetSubredditInfoUseCase,
    private val getSubredditRulesUseCase: GetSubredditRulesUseCase,
    private val fetchSubredditFeedUseCase: FetchSubredditFeedUseCase,
    private val voteUseCase: VoteUseCase,
    private val toggleSavePostUseCase: ToggleSavePostUseCase,
    private val searchPostsUseCase: SearchPostsUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SubredditDetailState())
    val state: StateFlow<SubredditDetailState> = _state.asStateFlow()

    fun handleIntent(intent: SubredditDetailIntent) {
        when (intent) {
            is SubredditDetailIntent.LoadSubreddit -> loadSubreddit(intent.name)
            is SubredditDetailIntent.Refresh -> refresh()
            is SubredditDetailIntent.LoadMore -> loadMore()
            is SubredditDetailIntent.ToggleSubscription -> toggleSubscription()
            is SubredditDetailIntent.VotePost -> votePost(intent.postId, intent.direction)
            is SubredditDetailIntent.SavePost -> savePost(intent.postId)
            is SubredditDetailIntent.SearchPosts -> searchPosts(intent.query)
            is SubredditDetailIntent.SetSearchActive -> setSearchActive(intent.active)
        }
    }

    private fun setSearchActive(active: Boolean) {
        _state.update { it.copy(isSearchActive = active) }
        if (!active) {
            _state.update { it.copy(searchQuery = "", searchResults = emptyList()) }
        }
    }

    private fun searchPosts(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }

        viewModelScope.launch {
            val subreddit = _state.value.subreddit?.name ?: return@launch
            _state.update { it.copy(isLoading = true) }
            searchPostsUseCase(subreddit, query)
                .onSuccess { results ->
                    _state.update { it.copy(searchResults = results, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun votePost(postId: String, direction: VoteDirection) {
        val post = _state.value.posts.find { it.id == postId } ?: return
        val finalDirection = if (post.voteStatus == direction) VoteDirection.NONE else direction

        viewModelScope.launch {
            voteUseCase(postId, post.voteStatus, direction).onSuccess {
                _state.update { state ->
                    state.copy(
                        posts = state.posts.map {
                            if (it.id == postId) {
                                val scoreDiff = finalDirection.value - it.voteStatus.value
                                it.copy(
                                    voteStatus = finalDirection,
                                    score = it.score + scoreDiff
                                )
                            } else it
                        }
                    )
                }
            }
        }
    }

    private fun savePost(postId: String) {
        viewModelScope.launch {
            val post = _state.value.posts.find { it.id == postId } ?: return@launch
            toggleSavePostUseCase(post).onSuccess {
                _state.update { state ->
                    state.copy(
                        posts = state.posts.map {
                            if (it.id == postId) it.copy(isSaved = !it.isSaved) else it
                        }
                    )
                }
            }
        }
    }

    private fun loadSubreddit(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val infoResult = getSubredditInfoUseCase(name)
            val rulesResult = getSubredditRulesUseCase(name)

            infoResult.onSuccess { subreddit ->
                val rules = rulesResult.getOrDefault(emptyList())
                _state.update { it.copy(subreddit = subreddit.copy(rules = rules)) }
                loadPosts(name)
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadPosts(name: String, after: String? = null) {
        viewModelScope.launch {
            fetchSubredditFeedUseCase(name, after)
                .onSuccess { paginatedData ->
                    _state.update {
                        it.copy(
                            posts = if (after == null) paginatedData.items else it.posts + paginatedData.items,
                            after = paginatedData.after,
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, isRefreshing = false, isPaginating = false, error = e.message) }
                }
        }
    }

    private fun refresh() {
        val name = _state.value.subreddit?.name ?: return
        _state.update { it.copy(isRefreshing = true) }
        loadPosts(name)
    }

    private fun loadMore() {
        val name = _state.value.subreddit?.name ?: return
        val after = _state.value.after ?: return
        if (_state.value.isPaginating) return

        _state.update { it.copy(isPaginating = true) }
        loadPosts(name, after)
    }

    private fun toggleSubscription() {
        val subreddit = _state.value.subreddit ?: return
        viewModelScope.launch {
            toggleSubscriptionUseCase(subreddit.name, subreddit.isSubscribed)
                .onSuccess {
                    _state.update {
                        it.copy(subreddit = subreddit.copy(isSubscribed = !subreddit.isSubscribed))
                    }
                }
        }
    }
}
