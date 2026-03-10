package com.rozetka.reditlite



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.reditlite.data.AuthState
import com.rozetka.reditlite.data.RedditRepository
import com.rozetka.reditlite.data.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: RedditRepository = RedditRepository()
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun authenticate(code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.getAccessToken(code).fold(
                onSuccess = { token ->
                    TokenStorage.accessToken = token
                    _authState.value = AuthState.Authenticated
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Authentication failed")
                }
            )
        }
    }
}