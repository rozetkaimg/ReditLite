package com.rozetka.presentation.ui.screen.subreddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.FeedType
import com.rozetka.domain.repository.FeedRepository
import com.rozetka.domain.repository.SubredditRepository
import com.rozetka.presentation.mvi.SubredditDetailIntent
import com.rozetka.presentation.mvi.SubredditDetailState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubredditDetailViewModel(
    private val subredditRepository: SubredditRepository,
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubredditDetailState())
    val state: StateFlow<SubredditDetailState> = _state.asStateFlow()

    fun handleIntent(intent: SubredditDetailIntent) {
        when (intent) {
            is SubredditDetailIntent.LoadSubreddit -> loadSubreddit(intent.name)
            is SubredditDetailIntent.Refresh -> refresh()
            is SubredditDetailIntent.LoadMore -> loadMore()
            is SubredditDetailIntent.ToggleSubscription -> toggleSubscription()
        }
    }

    private fun loadSubreddit(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val infoResult = subredditRepository.getSubredditInfo(name)
            val rulesResult = subredditRepository.getSubredditRules(name)

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
            val feedType = FeedType.Subreddit(name)
            val result = feedRepository.fetchFeed(feedType, after)
            
            result.onSuccess { paginatedData ->
                _state.update { 
                    it.copy(
                        posts = if (after == null) paginatedData.items else it.posts + paginatedData.items,
                        after = paginatedData.after,
                        isLoading = false,
                        isRefreshing = false,
                        isPaginating = false
                    )
                }
            }.onFailure { e ->
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
            val result = if (subreddit.isSubscribed) {
                subredditRepository.unsubscribe(subreddit.name)
            } else {
                subredditRepository.subscribe(subreddit.name)
            }
            
            result.onSuccess {
                _state.update { 
                    it.copy(subreddit = subreddit.copy(isSubscribed = !subreddit.isSubscribed))
                }
            }
        }
    }
}
