package com.pelypets.todolist.auth

import androidx.compose.runtime.staticCompositionLocalOf

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val message: String?) : GoogleSignInResult
}

/** Platform-specific Google account picker that returns an ID token for the backend to verify. */
fun interface GoogleSignInProvider {
    suspend fun signIn(): GoogleSignInResult
}

object UnavailableGoogleSignInProvider : GoogleSignInProvider {
    override suspend fun signIn() = GoogleSignInResult.Failure("Google Sign-In is not configured for this build")
}

val LocalGoogleSignInProvider = staticCompositionLocalOf<GoogleSignInProvider> { UnavailableGoogleSignInProvider }
