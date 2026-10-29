package com.pelypets.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pelypets.todolist.auth.rememberGoogleSignInProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(googleSignInProvider = rememberGoogleSignInProvider(BuildConfig.GOOGLE_WEB_CLIENT_ID))
        }
    }
}
