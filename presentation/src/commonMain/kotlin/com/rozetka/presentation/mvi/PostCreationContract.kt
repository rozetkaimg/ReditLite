package com.rozetka.presentation.mvi

import com.rozetka.domain.model.Subreddit

interface PostCreationContract {
    data class State(
        val isLoading: Boolean = false,
        val selectedSubreddit: String = "",
        val title: String = "",
        val content: String = "",
        val postType: PostType = PostType.TEXT,
        val mediaUri: String? = null,
        val error: String? = null,
        val isSuccess: Boolean = false
    )

    sealed class Event {
        data class OnSubredditSelected(val subreddit: String) : Event()
        data class OnTitleChanged(val title: String) : Event()
        data class OnContentChanged(val content: String) : Event()
        data class OnPostTypeChanged(val type: PostType) : Event()
        data class OnMediaSelected(val uri: String) : Event()
        object OnSubmitClicked : Event()
        object OnDismissError : Event()
    }

    sealed class Effect {
        object PostCreated : Effect()
        data class ShowError(val message: String) : Effect()
    }

    enum class PostType {
        TEXT, LINK, IMAGE
    }
}
