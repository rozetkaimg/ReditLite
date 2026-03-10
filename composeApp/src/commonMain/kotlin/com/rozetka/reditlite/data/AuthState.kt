package com.rozetka.reditlite.data

sealed interface AuthState {
    data object Initial : AuthState
    data object Loading : AuthState
    data object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}