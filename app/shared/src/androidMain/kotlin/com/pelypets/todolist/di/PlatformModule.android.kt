package com.pelypets.todolist.di

import com.liftric.kvault.KVault
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single { KVault(androidContext(), "todolist_secure_prefs") }
}

actual val defaultApiBaseUrl: String = "http://10.0.2.2:8080"
