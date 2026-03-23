package com.rozetka.presentation.ui.screen.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.FeedType
import com.rozetka.domain.usecase.feed.FetchFeedUseCase
import com.rozetka.domain.usecase.feed.ObserveFeedUseCase
import com.rozetka.presentation.mvi.FeedIntent
import com.rozetka.presentation.mvi.FeedState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val observeFeedUseCase: ObserveFeedUseCase,
    private val fetchFeedUseCase: FetchFeedUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<FeedState>(FeedState.Loading)
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentAfter: String? = null
    private var currentFeedType = FeedType.HOT

    init {
        processIntent(FeedIntent.LoadInitial(currentFeedType))
    }

    fun processIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.LoadInitial -> {
                currentFeedType = intent.feedType
                _state.value = FeedState.Loading
                loadFeed()
            }
            is FeedIntent.Refresh -> {
                currentFeedType = intent.feedType
                refresh()
            }
            is FeedIntent.LoadNextPage -> {
                if (currentFeedType == intent.feedType) {
                    loadNextPage()
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun loadFeed() {
        viewModelScope.launch {
            observeFeedUseCase(currentFeedType).collect { posts ->
                if (posts.isNotEmpty()) {
                    val currentState = _state.value
                    val paginating = (currentState as? FeedState.Content)?.isPaginating ?: false
                    _state.value = FeedState.Content(posts, paginating, currentAfter)
                } else if (_state.value is FeedState.Loading) {
                    refresh()
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            fetchFeedUseCase(currentFeedType, null).fold(
                onSuccess = { data ->
                    currentAfter = data.after
                    if (data.items.isEmpty()) {
                        _state.value = FeedState.Empty
                    } else {
                        _state.value = FeedState.Content(data.items, false, currentAfter)
                    }
                },
                onFailure = { error ->
                    if (_state.value !is FeedState.Content) {
                        _state.value = FeedState.Error(error.message ?: "Unknown error")
                    } else {
                        // Принудительно пересобираем стейт, чтобы отключилась анимация PullToRefresh
                        val currentState = _state.value as FeedState.Content
                        _state.value = currentState.copy()
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
                    onSuccess = { data ->
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