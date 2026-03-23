package com.rozetka.data.di

import com.rozetka.data.local.AppDatabase
import com.rozetka.data.local.SecureStorageManager
import com.rozetka.data.remote.createHttpClient
import com.rozetka.data.repository.AuthRepositoryImpl
import com.rozetka.data.repository.FeedRepositoryImpl
import com.rozetka.data.repository.PostRepositoryImpl
import com.rozetka.data.repository.SubredditRepositoryImpl
import com.rozetka.data.repository.UserRepositoryImpl
import com.rozetka.domain.repository.AuthRepository
import com.rozetka.domain.repository.FeedRepository
import com.rozetka.domain.repository.PostRepository
import com.rozetka.domain.repository.SubredditRepository
import com.rozetka.domain.repository.UserRepository
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val dataModule = module {
    includes(platformDataModule)

    single { Settings() }
    single { SecureStorageManager(get()) }
    single { createHttpClient(get()) }

    single { get<AppDatabase>().postDao() }

    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<FeedRepository> { FeedRepositoryImpl(get(), get()) }
    single<PostRepository> { PostRepositoryImpl(get(), get()) }
    single<SubredditRepository> { SubredditRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get()) }
}