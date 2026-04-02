package com.rozetka.domain.model


sealed interface AuthState {
    object Initial : AuthState
    object Loading : AuthState
    object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}