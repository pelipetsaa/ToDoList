package com.pelypets.todolist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.pelypets.todolist.auth.GoogleSignInProvider
import com.pelypets.todolist.auth.LocalGoogleSignInProvider
import com.pelypets.todolist.auth.UnavailableGoogleSignInProvider
import com.pelypets.todolist.core.ui.theme.TodoTheme
import com.pelypets.todolist.navigation.AppNavHost

@Composable
fun App(googleSignInProvider: GoogleSignInProvider = UnavailableGoogleSignInProvider) {
    CompositionLocalProvider(LocalGoogleSignInProvider provides googleSignInProvider) {
        TodoTheme {
            AppNavHost()
        }
    }
}
