package com.pelypets.todolist.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Google sign-in through Credential Manager. [webClientId] is the OAuth client of type "Web application":
 * the ID token is issued for it, and the backend accepts it as an audience.
 */
class CredentialManagerGoogleSignInProvider(private val activityContext: Context, private val webClientId: String) : GoogleSignInProvider {

    override suspend fun signIn(): GoogleSignInResult {
        if (webClientId.isBlank()) return UnavailableGoogleSignInProvider.signIn()
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleSignInResult.Failure("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(e.message)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(e.message)
        }
    }
}

/** Credential Manager needs an Activity context to show its bottom sheet. */
@Composable
fun rememberGoogleSignInProvider(webClientId: String): GoogleSignInProvider {
    val context = LocalContext.current
    return remember(context, webClientId) { CredentialManagerGoogleSignInProvider(context, webClientId) }
}
