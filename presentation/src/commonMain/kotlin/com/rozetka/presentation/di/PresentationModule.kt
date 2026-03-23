package com.rozetka.presentation.di

import com.rozetka.presentation.ui.screen.feed.FeedViewModel
import com.rozetka.presentation.ui.screen.login.AuthViewModel
import com.rozetka.presentation.ui.screen.postDetail.PostDetailViewModel
import com.rozetka.presentation.ui.screen.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val presentationModule = module {
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { FeedViewModel(get(), get()) }
    viewModel { PostDetailViewModel(get()) }
    viewModel { FeedViewModel(get(), get()) }
    viewModel { ProfileViewModel(get()) }
}
