package com.rozetka.presentation.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.SavedPost
import com.rozetka.domain.model.Trophy
import com.rozetka.domain.model.UserProfile
import com.rozetka.domain.usecase.auth.LogoutUseCase
import com.rozetka.domain.usecase.subreddit.GetProfileUseCase
import com.rozetka.domain.repository.UserRepository
import com.rozetka.presentation.mvi.ProfileEffect
import com.rozetka.presentation.mvi.ProfileIntent
import com.rozetka.presentation.mvi.ProfileState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val userRepository: UserRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    private var lastUsername: String? = null

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> {
                lastUsername = intent.username
                loadProfileData(intent.username)
            }
            is ProfileIntent.Logout -> logout()
            is ProfileIntent.Refresh -> loadProfileData(lastUsername)
        }
    }

    private fun loadProfileData(username: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val profileResult = if (username == null) {
                getProfileUseCase()
            } else {
                userRepository.getUserProfile(username)
            }

            profileResult.fold(
                onSuccess = { profile ->
                    _state.update { it.copy(profile = profile) }
                    loadTrophiesAndPosts(profile.name, isOwnProfile = username == null)
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    private suspend fun loadTrophiesAndPosts(username: String, isOwnProfile: Boolean) {
        val trophiesResult = if (isOwnProfile) userRepository.getTrophies() else Result.success(emptyList())
        val savedPostsResult = if (isOwnProfile) userRepository.getSavedPosts(username) else Result.success(emptyList())
        val userPostsResult = userRepository.getUserPosts(username)

        _state.update { state ->
            state.copy(
                isLoading = false,
                trophies = trophiesResult.getOrDefault(emptyList()),
                savedPosts = savedPostsResult.getOrDefault(emptyList()),
                userPosts = userPostsResult.getOrDefault(emptyList())
            )
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effect.emit(ProfileEffect.NavigateToLogin)
        }
    }
}
