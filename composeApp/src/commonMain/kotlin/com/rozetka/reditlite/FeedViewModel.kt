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
    private val repository: RedditRepository = RedditRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<FeedState>(FeedState.Loading)
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .flatMapLatest { query ->
                    flow {
                        emit(FeedState.Loading)
                        emit(repository.fetchPosts(query = query, after = null))
                    }
                }
                .collect { result ->
                    if (result is FeedState.Loading) {
                        _state.value = FeedState.Loading
                    } else if (result is Result<*>) {
                        val res = result as Result<Pair<List<RedditPost>, String?>>
                        res.fold(
                            onSuccess = { (posts, after) ->
                                if (posts.isEmpty()) {
                                    _state.value = FeedState.Empty
                                } else {
                                    _state.value = FeedState.Content(posts, false, after)
                                }
                            },
                            onFailure = { error ->
                                _state.value = FeedState.Error(error.message ?: "Unknown error")
                            }
                        )
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun retry() {
        _searchQuery.value = _searchQuery.value
    }

    fun loadNextPage() {
        val currentState = _state.value
        if (currentState is FeedState.Content && !currentState.isPaginating && currentState.nextAfter != null) {
            viewModelScope.launch {
                _state.value = currentState.copy(isPaginating = true)
                repository.fetchPosts(query = _searchQuery.value, after = currentState.nextAfter).fold(
                    onSuccess = { (newPosts, newAfter) ->
                        val updatedList = currentState.posts + newPosts
                        _state.value = FeedState.Content(updatedList, false, newAfter)
                    },
                    onFailure = {
                        _state.value = currentState.copy(isPaginating = false)
                    }
                )
            }
        }
    }
}