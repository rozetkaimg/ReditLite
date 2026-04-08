package com.rozetka.presentation.ui.screen.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.PaginatedData
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import com.rozetka.domain.usecase.feed.FetchFeedUseCase
import com.rozetka.domain.usecase.feed.ObserveFeedUseCase
import com.rozetka.domain.usecase.post.ToggleSavePostUseCase
import com.rozetka.domain.usecase.post.VoteUseCase
import com.rozetka.presentation.mvi.FeedIntent
import com.rozetka.presentation.mvi.FeedState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val observeFeedUseCase: ObserveFeedUseCase,
    private val fetchFeedUseCase: FetchFeedUseCase,
    private val voteUseCase: VoteUseCase,
    private val toggleSavePostUseCase: ToggleSavePostUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<FeedState>(FeedState.Loading)
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFeedType = MutableStateFlow<FeedType>(FeedType.HOT)
    val currentFeedType: StateFlow<FeedType> = _currentFeedType.asStateFlow()

    private var currentAfter: String? = null

    private val localVoteModifications = MutableStateFlow<Map<String, Pair<VoteDirection, Int>>>(emptyMap())

    init {
        observeFeed()
        observeSearchQuery()
        processIntent(FeedIntent.LoadInitial(FeedType.HOT))
    }

    private fun observeFeed() {
        _currentFeedType
            .flatMapLatest { feedType ->
                combine(observeFeedUseCase(feedType), localVoteModifications) { posts, localVotes ->
                    posts.map { post ->
                        localVotes[post.id]?.let { (newVoteStatus, scoreDiff) ->
                            post.copy(
                                voteStatus = newVoteStatus,
                                score = post.score + scoreDiff
                            )
                        } ?: post
                    }
                }
            }
            .onEach { posts ->
                val currentState = _state.value
                if (posts.isNotEmpty()) {
                    val paginating = (currentState as? FeedState.Content)?.isPaginating ?: false
                    _state.value = FeedState.Content(
                        posts = posts,
                        isPaginating = paginating,
                        nextAfter = currentAfter
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSearchQuery() {
        _searchQuery
            .debounce(500L)
            .onEach { query ->
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else if (_state.value !is FeedState.Loading) {
                    refresh(showLoading = true)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _state.value = FeedState.Loading
            currentAfter = null
            fetchFeedUseCase.search(query, null).fold(
                onSuccess = { data: PaginatedData<Post> ->
                    currentAfter = data.after
                    _state.value = FeedState.Content(
                        posts = data.items,
                        isPaginating = false,
                        nextAfter = data.after
                    )
                },
                onFailure = { error ->
                    _state.value = FeedState.Error(error.message ?: "")
                }
            )
        }
    }

    fun processIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.LoadInitial -> {
                if (_searchQuery.value.isEmpty()) {
                    if (_currentFeedType.value != intent.feedType) {
                        currentAfter = null
                        _state.value = FeedState.Loading
                        _currentFeedType.value = intent.feedType
                        refresh(showLoading = true)
                    } else if (_state.value is FeedState.Loading) {
                        refresh(showLoading = true)
                    }
                }
            }
            is FeedIntent.Refresh -> {
                if (_searchQuery.value.isEmpty()) {
                    _currentFeedType.value = intent.feedType
                    refresh(showLoading = false)
                } else {
                    performSearch(_searchQuery.value)
                }
            }
            is FeedIntent.LoadNextPage -> {
                val currentState = _state.value
                if (currentState is FeedState.Content && currentState.isPaginating) return

                if (_searchQuery.value.isEmpty()) {
                    loadNextPage()
                } else {
                    loadNextSearchPage()
                }
            }
            is FeedIntent.Vote -> vote(intent.post, intent.direction)
            is FeedIntent.ToggleSave -> toggleSave(intent.post)
        }
    }

    private fun toggleSave(post: Post) {
        viewModelScope.launch {
            toggleSavePostUseCase(post)
        }
    }

    private fun vote(post: Post, direction: VoteDirection) {
        val finalDirection = if (post.voteStatus == direction) VoteDirection.NONE else direction

        val scoreDiff = when {
            post.voteStatus == VoteDirection.UP && finalDirection == VoteDirection.NONE -> -1
            post.voteStatus == VoteDirection.UP && finalDirection == VoteDirection.DOWN -> -2
            post.voteStatus == VoteDirection.DOWN && finalDirection == VoteDirection.NONE -> 1
            post.voteStatus == VoteDirection.DOWN && finalDirection == VoteDirection.UP -> 2
            post.voteStatus == VoteDirection.NONE && finalDirection == VoteDirection.UP -> 1
            post.voteStatus == VoteDirection.NONE && finalDirection == VoteDirection.DOWN -> -1
            else -> 0
        }

        val currentModifications = localVoteModifications.value.toMutableMap()
        currentModifications[post.id] = Pair(finalDirection, scoreDiff)
        localVoteModifications.value = currentModifications

        viewModelScope.launch {
            voteUseCase(post.id, post.voteStatus, direction).onFailure {
                val fallbackMods = localVoteModifications.value.toMutableMap()
                fallbackMods.remove(post.id)
                localVoteModifications.value = fallbackMods
            }
        }
    }

    private fun loadNextSearchPage() {
        val currentState = _state.value
        val query = _searchQuery.value
        if (currentState is FeedState.Content && !currentState.isPaginating && currentAfter != null) {
            viewModelScope.launch {
                _state.value = currentState.copy(isPaginating = true)
                fetchFeedUseCase.search(query, currentAfter).fold(
                    onSuccess = { data: PaginatedData<Post> ->
                        currentAfter = data.after
                        _state.value = currentState.copy(
                            posts = currentState.posts + data.items,
                            isPaginating = false,
                            nextAfter = data.after
                        )
                    },
                    onFailure = {
                        _state.value = currentState.copy(isPaginating = false)
                    }
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            val feedType = _currentFeedType.value
            if (showLoading && _state.value !is FeedState.Content) {
                _state.value = FeedState.Loading
            }

            currentAfter = null
            fetchFeedUseCase(feedType, null).onSuccess { data ->
                currentAfter = data.after
                if (data.items.isEmpty()) {
                    _state.value = FeedState.Content(
                        posts = emptyList(),
                        isPaginating = false,
                        nextAfter = null
                    )
                }
            }.onFailure { error ->
                if (_state.value !is FeedState.Content) {
                    _state.value = FeedState.Error(error.message ?: "")
                }
            }
        }
    }

    private fun loadNextPage() {
        val currentState = _state.value
        val feedType = _currentFeedType.value
        if (currentState is FeedState.Content && !currentState.isPaginating && currentAfter != null) {
            viewModelScope.launch {
                _state.value = currentState.copy(isPaginating = true)
                fetchFeedUseCase(feedType, currentAfter).fold(
                    onSuccess = { data: PaginatedData<Post> ->
                        currentAfter = data.after
                        val updatedState = _state.value
                        if (updatedState is FeedState.Content) {
                            _state.value = updatedState.copy(
                                isPaginating = false,
                                nextAfter = data.after
                            )
                        }
                    },
                    onFailure = {
                        val updatedState = _state.value
                        if (updatedState is FeedState.Content) {
                            _state.value = updatedState.copy(isPaginating = false)
                        }
                    }
                )
            }
        }
    }
}