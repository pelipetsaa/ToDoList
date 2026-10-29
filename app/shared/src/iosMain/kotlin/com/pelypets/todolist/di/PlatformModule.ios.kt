package com.pelypets.todolist.di

import com.liftric.kvault.KVault
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single { KVault(serviceName = "com.pelypets.todolist") }
}

actual val defaultApiBaseUrl: String = "http://localhost:8080"
