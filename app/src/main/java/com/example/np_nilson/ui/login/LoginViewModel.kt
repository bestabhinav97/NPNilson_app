package com.example.np_nilson.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.np_nilson.data.api.ApiClient
import com.example.np_nilson.data.api.ErrorResponse
import com.example.np_nilson.data.api.LoginRequest
import com.example.np_nilson.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isPasswordVisible by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var emailError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var showContactAdminDialog by mutableStateOf(false)
        private set

    var showSettingsDialog by mutableStateOf(false)
        private set

    var backendUrl by mutableStateOf(ApiClient.getBaseUrl())
        private set

    fun onEmailChanged(value: String) {
        email = value
        emailError = null
        errorMessage = null
    }

    fun onPasswordChanged(value: String) {
        password = value
        passwordError = null
        errorMessage = null
    }

    fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
    }

    fun onContactAdminClicked() {
        showContactAdminDialog = true
    }

    fun dismissContactAdminDialog() {
        showContactAdminDialog = false
    }

    fun onSettingsClicked() {
        showSettingsDialog = true
    }

    fun dismissSettingsDialog() {
        showSettingsDialog = false
    }

    fun updateBackendUrl(newUrl: String) {
        backendUrl = newUrl
        ApiClient.setBaseUrl(newUrl)
        showSettingsDialog = false
    }

    fun login(sessionManager: SessionManager, onSuccess: () -> Unit) {
        // Validation
        var hasError = false
        if (email.isBlank()) {
            emailError = "E-postadress krävs"
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailError = "Ange en giltig e-postadress"
            hasError = true
        }

        if (password.isBlank()) {
            passwordError = "Lösenord krävs"
            hasError = true
        }

        if (hasError) return

        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val response = ApiClient.getAuthApi().login(LoginRequest(email = email.trim(), password = password))
                isLoading = false
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionManager.saveSession(body.token, body.user)
                    onSuccess()
                } else {
                    val errorBodyStr = response.errorBody()?.string()
                    val parsedError = try {
                        Gson().fromJson(errorBodyStr, ErrorResponse::class.java)?.error
                    } catch (e: Exception) {
                        null
                    }
                    errorMessage = parsedError ?: "Felaktig e-postadress eller lösenord"
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Kan inte ansluta till servern. Kontrollera din anslutning eller serveradress."
            }
        }
    }
}
