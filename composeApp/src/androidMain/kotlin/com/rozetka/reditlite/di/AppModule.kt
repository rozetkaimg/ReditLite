package com.rozetka.reditlite.di

import com.rozetka.domain.work.PostWorkManager
import com.rozetka.reditlite.work.AndroidPostWorkManager
import org.koin.dsl.module

val appModule = module {
    single<PostWorkManager> { AndroidPostWorkManager(get()) }
}
