package com.rozetka.presentation.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.model.SavedPost
import com.rozetka.domain.model.Trophy
import com.rozetka.domain.model.UserProfile
import com.rozetka.domain.usecase.auth.LogoutUseCase
import com.rozetka.domain.usecase.subreddit.GetProfileUseCase
import com.rozetka.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val trophies: List<Trophy> = emptyList(),
    val savedPosts: List<SavedPost> = emptyList(),
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val userRepository: UserRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val profileResult = getProfileUseCase()
            profileResult.fold(
                onSuccess = { profile ->
                    _uiState.update { it.copy(profile = profile) }
                    loadTrophiesAndSaved(profile.name)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private suspend fun loadTrophiesAndSaved(username: String) {
        val trophiesResult = userRepository.getTrophies()
        val postsResult = userRepository.getSavedPosts(username)

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                trophies = trophiesResult.getOrDefault(emptyList()),
                savedPosts = postsResult.getOrDefault(emptyList())
            )
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLogoutComplete()
        }
    }
}