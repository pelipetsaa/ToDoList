package com.pelypets.todolist.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Implemented in Swift, where the GoogleSignIn SDK is available through Swift Package Manager. */
interface GoogleSignInBridge {
    fun signIn(onSuccess: (idToken: String) -> Unit, onCancel: () -> Unit, onError: (message: String) -> Unit)
}

class GoogleSignInBridgeProvider(private val bridge: GoogleSignInBridge?) : GoogleSignInProvider {

    override suspend fun signIn(): GoogleSignInResult {
        val bridge = bridge ?: return UnavailableGoogleSignInProvider.signIn()
        return suspendCancellableCoroutine { continuation ->
            fun complete(result: GoogleSignInResult) {
                if (continuation.isActive) continuation.resume(result)
            }
            bridge.signIn(
                onSuccess = { complete(GoogleSignInResult.Success(it)) },
                onCancel = { complete(GoogleSignInResult.Cancelled) },
                onError = { complete(GoogleSignInResult.Failure(it)) },
            )
        }
    }
}
