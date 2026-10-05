package com.example.np_nilson.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.np_nilson.data.api.ApiClient
import com.example.np_nilson.data.session.SessionManager
import com.example.np_nilson.ui.branding.NpNilssonFullLogo
import com.example.np_nilson.ui.theme.NpBluePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(
    sessionManager: SessionManager,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(Unit) {
        val token = sessionManager.getToken()
        if (token.isNullOrBlank()) {
            onNavigateToLogin()
        } else {
            val isSessionValid = withContext(Dispatchers.IO) {
                try {
                    val response = ApiClient.getAuthApi().getCurrentUser("Bearer $token")
                    if (response.isSuccessful && response.body() != null) {
                        sessionManager.saveSession(token, response.body()!!)
                        true
                    } else {
                        sessionManager.clearSession()
                        false
                    }
                } catch (e: Exception) {
                    // On network error or offline, if session exists allow proceeding or require login
                    sessionManager.isLoggedIn()
                }
            }
            if (isSessionValid) {
                onNavigateToHome()
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NpBluePrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NpNilssonFullLogo(isDarkBackground = true)

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Loading...",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}
