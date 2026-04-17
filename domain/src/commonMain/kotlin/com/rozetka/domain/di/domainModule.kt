package com.rozetka.domain.di

import com.rozetka.domain.usecase.auth.*
import com.rozetka.domain.usecase.feed.*
import com.rozetka.domain.usecase.post.*
import com.rozetka.domain.usecase.submit.*
import com.rozetka.domain.usecase.subreddit.*
import com.rozetka.domain.work.PostWorkManager
import org.koin.dsl.module

val domainModule = module {
    factory { ObserveFeedUseCase(get()) }
    factory { FetchFeedUseCase(get()) }
    factory { FetchSubredditFeedUseCase(get()) }
    factory { GetCommentsUseCase(get()) }
    factory { GetPostAndCommentsUseCase(get()) }
    factory { SubmitCommentUseCase(get()) }
    factory { VoteUseCase(get()) }
    factory { AuthenticateUseCase(get()) }
    factory { GetAuthStatusUseCase(get()) }
    factory { SetOnboardingCompletedUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { GetAuthUrlUseCase(get()) }
    factory { GetProfileUseCase(get()) }
    factory { GetMySubredditsUseCase(get()) }
    factory { GetSubredditInfoUseCase(get()) }
    factory { GetSubredditRulesUseCase(get()) }
    factory { ObserveMySubredditsUseCase(get()) }
    factory { ToggleSubscriptionUseCase(get()) }
    factory { ToggleFavoriteSubredditUseCase(get()) }
    factory { SearchSubredditsUseCase(get()) }
    factory { GetSavedPostsUseCase(get()) }
    factory { ToggleSavePostUseCase(get()) }
    factory { SearchPostsUseCase(get()) }
    factory { SubmitTextPostUseCase(get()) }
    factory { SubmitLinkPostUseCase(get()) }
    factory { SubmitImagePostWithBytesUseCase(get()) }
}