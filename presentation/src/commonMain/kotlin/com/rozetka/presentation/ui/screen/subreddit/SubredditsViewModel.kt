package com.rozetka.presentation.ui.screen.subreddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.usecase.subreddit.GetMySubredditsUseCase
import com.rozetka.domain.usecase.subreddit.ObserveMySubredditsUseCase
import com.rozetka.domain.usecase.subreddit.SearchSubredditsUseCase
import com.rozetka.domain.usecase.subreddit.ToggleFavoriteSubredditUseCase
import com.rozetka.domain.usecase.subreddit.ToggleSubscriptionUseCase
import com.rozetka.presentation.mvi.SubredditsEffect
import com.rozetka.presentation.mvi.SubredditsIntent
import com.rozetka.presentation.mvi.SubredditsState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubredditsViewModel(
    private val getMySubredditsUseCase: GetMySubredditsUseCase,
    private val observeMySubredditsUseCase: ObserveMySubredditsUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val toggleFavoriteSubredditUseCase: ToggleFavoriteSubredditUseCase,
    private val searchSubredditsUseCase: SearchSubredditsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SubredditsState())
    val state: StateFlow<SubredditsState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SubredditsEffect>()
    val effect: SharedFlow<SubredditsEffect> = _effect.asSharedFlow()

    init {
        observeSubreddits()
        handleIntent(SubredditsIntent.LoadSubreddits)
    }

    private fun observeSubreddits() {
        observeMySubredditsUseCase()
            .onEach { subreddits ->
                _state.update { currentState ->
                    val updatedSearchResults = currentState.searchResults.map { searchResult ->
                        val local = subreddits.find { it.name == searchResult.name }
                        if (local != null) {
                            searchResult.copy(
                                isSubscribed = local.isSubscribed,
                                isFavorite = local.isFavorite
                            )
                        } else {
                            searchResult
                        }
                    }
                    currentState.copy(
                        subreddits = subreddits,
                        searchResults = updatedSearchResults
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: SubredditsIntent) {
        when (intent) {
            is SubredditsIntent.LoadSubreddits -> loadSubreddits()
            is SubredditsIntent.SearchSubreddits -> searchSubreddits(intent.query)
            is SubredditsIntent.SetSearchActive -> {
                _state.update { it.copy(isSearchActive = intent.active) }
                if (!intent.active) {
                    searchSubreddits("")
                }
            }
            is SubredditsIntent.ToggleSubscription -> toggleSubscription(intent)
            is SubredditsIntent.ToggleFavorite -> toggleFavorite(intent)
            is SubredditsIntent.NavigateToSubreddit -> {
                viewModelScope.launch {
                    _effect.emit(SubredditsEffect.NavigateToSubreddit(intent.name))
                }
            }
        }
    }

    private fun loadSubreddits() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getMySubredditsUseCase()
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun toggleSubscription(intent: SubredditsIntent.ToggleSubscription) {
        viewModelScope.launch {
            toggleSubscriptionUseCase(intent.subreddit.name, intent.subreddit.isSubscribed)
                .onFailure { error ->
                    _effect.emit(SubredditsEffect.ShowError(error.message ?: "Subscription update failed"))
                }
        }
    }

    private fun toggleFavorite(intent: SubredditsIntent.ToggleFavorite) {
        viewModelScope.launch {
            toggleFavoriteSubredditUseCase(intent.subreddit.name, !intent.subreddit.isFavorite)
        }
    }

    private fun searchSubreddits(query: String) {
        if (query == _state.value.searchQuery && _state.value.searchResults.isNotEmpty()) return

        _state.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                _state.update { it.copy(searchResults = emptyList(), isLoading = false) }
                return@launch
            }
            _state.update { it.copy(isLoading = true) }
            searchSubredditsUseCase(query)
                .onSuccess { results ->
                    _state.update { it.copy(searchResults = results, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
