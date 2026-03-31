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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
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

    private var currentAfter: String? = null
    private var currentFeedType: FeedType = FeedType.HOT

    init {
        processIntent(FeedIntent.LoadInitial(currentFeedType))
        observeSearchQuery()
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .collect { query ->
                    if (query.isNotEmpty()) {
                        performSearch(query)
                    } else {
                        loadFeed()
                    }
                }
        }
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
                    _state.value = FeedState.Error(error.message ?: "Search failed")
                }
            )
        }
    }

    fun processIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.LoadInitial -> {
                if (_searchQuery.value.isEmpty()) {
                    currentFeedType = intent.feedType
                    _state.value = FeedState.Loading
                    loadFeed()
                }
            }
            is FeedIntent.Refresh -> {
                if (_searchQuery.value.isEmpty()) {
                    currentFeedType = intent.feedType
                    refresh()
                } else {
                    performSearch(_searchQuery.value)
                }
            }
            is FeedIntent.LoadNextPage -> {
                if (_searchQuery.value.isEmpty()) {
                    if (currentFeedType == intent.feedType) {
                        loadNextPage()
                    }
                } else {
                    loadNextSearchPage()
                }
            }
            is FeedIntent.Vote -> {
                vote(intent.post, intent.direction)
            }
            is FeedIntent.ToggleSave -> {
                toggleSave(intent.post)
            }
        }
    }

    private fun toggleSave(post: Post) {
        viewModelScope.launch {
            toggleSavePostUseCase(post.id, post.isSaved)
        }
    }

    private fun vote(post: Post, direction: VoteDirection) {
        viewModelScope.launch {
            voteUseCase(post.id, post.voteStatus, direction)
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

    private fun loadFeed() {
        viewModelScope.launch {
            observeFeedUseCase(currentFeedType).collect { posts ->
                val currentState = _state.value

                if (posts.isNotEmpty()) {
                    val paginating = (currentState as? FeedState.Content)?.isPaginating ?: false
                    val nextAfter = (currentState as? FeedState.Content)?.nextAfter ?: currentAfter
                    _state.value = FeedState.Content(posts, paginating, nextAfter)
                } else if (currentState is FeedState.Loading) {
                    refresh()
                } else if (currentState is FeedState.Content && posts.isEmpty()) {
                    _state.value = FeedState.Content(emptyList(), false, null)
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            if (_state.value !is FeedState.Content) {
                _state.value = FeedState.Loading
            }

            currentAfter = null
            fetchFeedUseCase(currentFeedType, null).fold(
                onSuccess = { data: PaginatedData<Post> ->
                    currentAfter = data.after
                },
                onFailure = { error ->
                    if (_state.value !is FeedState.Content || (_state.value as? FeedState.Content)?.posts?.isEmpty() == true) {
                        _state.value = FeedState.Error(error.message ?: "Unknown error")
                    }
                }
            )
        }
    }

    private fun loadNextPage() {
        val currentState = _state.value
        if (currentState is FeedState.Content && !currentState.isPaginating && currentAfter != null) {
            viewModelScope.launch {
                _state.value = currentState.copy(isPaginating = true)
                fetchFeedUseCase(currentFeedType, currentAfter).fold(
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