package com.rozetka.presentation.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.domain.usecase.auth.AuthenticateUseCase
import com.rozetka.domain.usecase.auth.GetAuthStatusUseCase
import com.rozetka.domain.usecase.auth.SetOnboardingCompletedUseCase
import com.rozetka.presentation.mvi.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authenticateUseCase: AuthenticateUseCase,
    private val getAuthStatusUseCase: GetAuthStatusUseCase,
    private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            val status = getAuthStatusUseCase()
            if (status is AuthState.Authenticated) {
                _authState.value = AuthState.Authenticated
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            setOnboardingCompletedUseCase()
        }
    }

    fun authenticate(code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authenticateUseCase(code).fold(
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