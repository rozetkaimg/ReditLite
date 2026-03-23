package com.rozetka.presentation.mvi

sealed interface AuthState {
    data object Initial : AuthState
    data object Loading : AuthState
    data object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}

sealed interface AuthIntent {
    data object CheckAuth : AuthIntent
    data class Authenticate(val code: String) : AuthIntent
    data object Logout : AuthIntent
}
