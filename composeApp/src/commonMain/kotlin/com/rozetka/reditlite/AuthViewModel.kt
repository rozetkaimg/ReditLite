package com.rozetka.reditlite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.reditlite.data.AuthState
import com.rozetka.reditlite.data.RedditRepository
import com.rozetka.reditlite.data.SecureStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: RedditRepository,
    private val storageManager: SecureStorageManager
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        if (!storageManager.isFirstLaunch && storageManager.accessToken != null) {
            _authState.value = AuthState.Authenticated
        }
    }

    fun completeOnboarding() {
        storageManager.isFirstLaunch = false
    }

    fun authenticate(code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.getAccessToken(code).fold(
                onSuccess = {
                    _authState.value = AuthState.Authenticated
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Authentication failed")
                }
            )
        }
    }
}