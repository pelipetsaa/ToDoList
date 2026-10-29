package com.pelypets.todolist

import android.app.Application
import com.pelypets.todolist.data.remote.ApiConfig
import com.pelypets.todolist.di.initKoin
import org.koin.android.ext.koin.androidContext

class TodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(ApiConfig(baseUrl = BuildConfig.API_BASE_URL, enableLogging = BuildConfig.DEBUG)) {
            androidContext(this@TodoApplication)
        }
    }
}
