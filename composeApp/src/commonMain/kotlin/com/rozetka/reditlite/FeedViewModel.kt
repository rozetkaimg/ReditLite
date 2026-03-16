package com.rozetka.reditlite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.reditlite.data.RedditRepository
import com.rozetka.reditlite.models.RedditPost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface FeedState {
    object Loading : FeedState
    data class Content(
        val posts: List<RedditPost>,
        val isPaginating: Boolean = false,
        val nextAfter: String? = null
    ) : FeedState
    data class Error(val message: String) : FeedState
    object Empty : FeedState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val repository: RedditRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FeedState>(FeedState.Loading)
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentAfter: String? = null

    init {
        viewModelScope.launch {
            repository.getPostsFlow().collect { posts ->
                if (posts.isNotEmpty()) {
                    val currentState = _state.value
                    val paginating = (currentState as? FeedState.Content)?.isPaginating ?: false
                    _state.value = FeedState.Content(posts, paginating, currentAfter)
                } else if (_state.value !is FeedState.Loading) {
                    _state.value = FeedState.Empty
                }
            }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .collectLatest { query ->
                    _state.value = FeedState.Loading
                    currentAfter = null
                    repository.fetchAndCachePosts(query = query, after = null).fold(
                        onSuccess = { after -> currentAfter = after },
                        onFailure = { error -> _state.value = FeedState.Error(error.message ?: "Unknown error") }
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            currentAfter = null
            repository.fetchAndCachePosts(query = _searchQuery.value, after = null).onSuccess {
                currentAfter = it
            }
        }
    }

    fun retry() {
        refresh()
    }

    fun loadNextPage() {
        val currentState = _state.value
        if (currentState is FeedState.Content && !currentState.isPaginating && currentAfter != null) {
            viewModelScope.launch {
                _state.value = currentState.copy(isPaginating = true)
                repository.fetchAndCachePosts(query = _searchQuery.value, after = currentAfter).fold(
                    onSuccess = { after ->
                        currentAfter = after
                        _state.value = (_state.value as FeedState.Content).copy(isPaginating = false, nextAfter = after)
                    },
                    onFailure = {
                        _state.value = currentState.copy(isPaginating = false)
                    }
                )
            }
        }
    }
}