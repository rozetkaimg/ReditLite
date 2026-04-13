package com.rozetka.presentation.mvi

import com.rozetka.domain.model.Post
import com.rozetka.domain.model.Trophy
import com.rozetka.domain.model.UserProfile

data class ProfileState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val trophies: List<Trophy> = emptyList(),
    val savedPosts: List<Post> = emptyList(),
    val userPosts: List<Post> = emptyList(),
    val error: String? = null
)

sealed class ProfileIntent {
    data class LoadProfile(val username: String? = null) : ProfileIntent()
    data object Logout : ProfileIntent()
    data object Refresh : ProfileIntent()
}

sealed class ProfileEffect {
    data object NavigateToLogin : ProfileEffect()
    data class ShowError(val message: String) : ProfileEffect()
}
