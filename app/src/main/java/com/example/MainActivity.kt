package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MainFeatureScreen
import com.example.ui.theme.SakuTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    var activeUserId by remember { mutableStateOf<String?>(Firebase.auth.currentUser?.uid ?: Firebase.auth.currentUser?.email) }
    var isDarkTheme by remember { mutableStateOf(true) } // Default to dark mode matching app icon style

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                activeUserId = auth.currentUser!!.email ?: auth.currentUser!!.uid
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    SakuTheme(darkTheme = isDarkTheme) {
        val userId = activeUserId
        if (userId == null) {
            AuthScreen(
                onAuthSuccess = { customUid ->
                    activeUserId = customUid ?: Firebase.auth.currentUser?.email ?: Firebase.auth.currentUser?.uid ?: "user_saku_demo"
                }
            )
        } else {
            key(userId) {
                MainFeatureScreen(
                    currentUserId = userId,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = !isDarkTheme },
                    onSignOut = {
                        try {
                            Firebase.auth.signOut()
                        } catch (e: Exception) {}
                        activeUserId = null
                    }
                )
            }
        }
    }
}
