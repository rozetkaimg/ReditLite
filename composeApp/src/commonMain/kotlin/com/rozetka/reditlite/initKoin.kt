package com.rozetka.reditlite


import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import com.rozetka.presentation.di.presentationModule
import com.rozetka.data.di.dataModule
import com.rozetka.domain.di.domainModule

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        presentationModule,
        dataModule,
        domainModule
    )
}