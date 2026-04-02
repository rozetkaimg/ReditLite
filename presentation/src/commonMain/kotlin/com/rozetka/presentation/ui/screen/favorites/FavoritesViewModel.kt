package com.rozetka.presentation.ui.screen.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.usecase.post.GetSavedPostsUseCase
import com.rozetka.domain.usecase.post.ToggleSavePostUseCase
import com.rozetka.domain.usecase.subreddit.ObserveMySubredditsUseCase
import com.rozetka.domain.usecase.subreddit.ToggleFavoriteSubredditUseCase
import com.rozetka.presentation.mvi.FavoritesIntent
import com.rozetka.presentation.mvi.FavoritesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val getSavedPostsUseCase: GetSavedPostsUseCase,
    private val toggleSavePostUseCase: ToggleSavePostUseCase,
    private val observeMySubredditsUseCase: ObserveMySubredditsUseCase,
    private val toggleFavoriteSubredditUseCase: ToggleFavoriteSubredditUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        combine(
            getSavedPostsUseCase(),
            observeMySubredditsUseCase()
        ) { posts, subreddits ->
            FavoritesState(
                savedPosts = posts,
                favoriteSubreddits = subreddits.filter { it.isFavorite }
            )
        }.onEach { newState ->
            _state.value = newState
        }.launchIn(viewModelScope)
    }

    fun handleIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.LoadFavorites -> {
                // Already handled by observeFavorites init
            }
            is FavoritesIntent.TogglePostSave -> togglePostSave(intent)
            is FavoritesIntent.ToggleSubredditFavorite -> toggleSubredditFavorite(intent)
        }
    }

    private fun togglePostSave(intent: FavoritesIntent.TogglePostSave) {
        viewModelScope.launch {
            toggleSavePostUseCase(intent.post.id, intent.post.isSaved)
        }
    }

    private fun toggleSubredditFavorite(intent: FavoritesIntent.ToggleSubredditFavorite) {
        viewModelScope.launch {
            toggleFavoriteSubredditUseCase(intent.subreddit.name, !intent.subreddit.isFavorite)
        }
    }
}
