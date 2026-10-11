package com.example.np_nilson.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.np_nilson.data.api.ReportCategory
import com.example.np_nilson.data.session.SessionManager
import com.example.np_nilson.ui.home.HomeScreen
import com.example.np_nilson.ui.login.LoginScreen
import com.example.np_nilson.ui.report.CreateReportScreen
import com.example.np_nilson.ui.splash.SplashScreen

enum class ScreenState {
    SPLASH,
    LOGIN,
    HOME,
    CREATE_REPORT
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var currentScreen by remember { mutableStateOf(ScreenState.SPLASH) }
    var reportCategory by remember { mutableStateOf<ReportCategory?>(null) }

    when (currentScreen) {
        ScreenState.SPLASH -> {
            SplashScreen(
                sessionManager = sessionManager,
                onNavigateToLogin = { currentScreen = ScreenState.LOGIN },
                onNavigateToHome = { currentScreen = ScreenState.HOME }
            )
        }
        ScreenState.LOGIN -> {
            LoginScreen(
                sessionManager = sessionManager,
                onLoginSuccess = { currentScreen = ScreenState.HOME }
            )
        }
        ScreenState.HOME -> {
            HomeScreen(
                sessionManager = sessionManager,
                onLoggedOut = { currentScreen = ScreenState.LOGIN },
                onCreateReport = { category ->
                    reportCategory = category
                    currentScreen = ScreenState.CREATE_REPORT
                }
            )
        }
        ScreenState.CREATE_REPORT -> {
            CreateReportScreen(
                sessionManager = sessionManager,
                initialCategory = reportCategory,
                onClose = { currentScreen = ScreenState.HOME }
            )
        }
    }
}
