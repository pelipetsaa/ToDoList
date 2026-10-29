package com.pelypets.todolist.di

import com.pelypets.todolist.data.remote.ApiConfig
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** Provides the HTTP engine and the KVault instance for the current platform. */
expect fun platformModule(): Module

/** Backend URL reachable from the emulator/simulator when the server runs on the host machine. */
expect val defaultApiBaseUrl: String

fun initKoin(
    apiConfig: ApiConfig = ApiConfig(defaultApiBaseUrl),
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(platformModule(), dataModule(apiConfig), domainModule, presentationModule)
}
