package com.pelypets.todolist

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.pelypets.todolist.auth.GoogleSignInBridge
import com.pelypets.todolist.auth.GoogleSignInBridgeProvider
import com.pelypets.todolist.data.remote.ApiConfig
import com.pelypets.todolist.di.defaultApiBaseUrl
import com.pelypets.todolist.di.initKoin
import platform.UIKit.UIViewController

private var koinStarted = false

/**
 * Entry point for the iOS app. [googleSignIn] is implemented in Swift on top of the GoogleSignIn SDK
 * and is null when no client ID is configured.
 */
fun MainViewController(apiBaseUrl: String?, googleSignIn: GoogleSignInBridge?): UIViewController {
    if (!koinStarted) {
        initKoin(ApiConfig(baseUrl = apiBaseUrl?.takeIf { it.isNotBlank() } ?: defaultApiBaseUrl))
        koinStarted = true
    }
    return ComposeUIViewController {
        val provider = remember(googleSignIn) { GoogleSignInBridgeProvider(googleSignIn) }
        App(googleSignInProvider = provider)
    }
}
