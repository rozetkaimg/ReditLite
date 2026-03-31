package com.rozetka.presentation.di

import com.rozetka.presentation.ui.screen.favorites.FavoritesViewModel
import com.rozetka.presentation.ui.screen.feed.FeedViewModel
import com.rozetka.presentation.ui.screen.login.AuthViewModel
import com.rozetka.presentation.ui.screen.postDetail.PostDetailViewModel
import com.rozetka.presentation.ui.screen.profile.ProfileViewModel
import com.rozetka.presentation.ui.screen.subreddit.SubredditDetailViewModel
import com.rozetka.presentation.ui.screen.subreddit.SubredditsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val presentationModule = module {
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { FeedViewModel(get(), get(), get(), get()) }
    viewModel { PostDetailViewModel(get()) }
    viewModel { ProfileViewModel(get(), get(), get()) }
    viewModel { SubredditsViewModel(get(), get(), get(), get()) }
    viewModel { FavoritesViewModel(get(), get(), get(), get()) }
    viewModel { SubredditDetailViewModel(get(), get()) }
}
