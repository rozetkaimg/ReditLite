package com.rozetka.domain.di

import com.rozetka.domain.usecase.auth.*
import com.rozetka.domain.usecase.feed.*
import com.rozetka.domain.usecase.post.*
import com.rozetka.domain.usecase.subreddit.*
import org.koin.dsl.module

val domainModule = module {
    factory { ObserveFeedUseCase(get()) }
    factory { FetchFeedUseCase(get()) }

    factory { GetCommentsUseCase(get()) }
    factory { VoteUseCase(get()) }

    factory { AuthenticateUseCase(get()) }
    factory { GetAuthStatusUseCase(get()) }
    factory { SetOnboardingCompletedUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { GetAuthUrlUseCase(get()) }

    factory { GetProfileUseCase(get()) }

    factory { GetMySubredditsUseCase(get()) }
}