package com.example.np_nilson.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.np_nilson.data.api.ApiClient
import com.example.np_nilson.data.api.CreateUserRequest
import com.example.np_nilson.data.api.ErrorResponse
import com.example.np_nilson.data.api.ReportDto
import com.example.np_nilson.data.api.UpdateReportStatusRequest
import com.example.np_nilson.data.api.ResetPasswordRequest
import com.example.np_nilson.data.api.UserDto
import com.example.np_nilson.data.report.ReportImageRepository
import com.example.np_nilson.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    var currentUser by mutableStateOf<UserDto?>(null)
        private set

    var usersList by mutableStateOf<List<UserDto>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    // Reports (own reports for USER, all reports for ADMIN)
    var reports by mutableStateOf<List<ReportDto>>(emptyList())
        private set

    var isLoadingReports by mutableStateOf(false)
        private set

    var reportsError by mutableStateOf<String?>(null)
        private set

    var selectedReport by mutableStateOf<ReportDto?>(null)
        private set

    var isUpdatingReportStatus by mutableStateOf(false)
        private set

    var reportStatusError by mutableStateOf<String?>(null)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    var isErrorStatus by mutableStateOf(false)
        private set

    // Create User Form State
    var showCreateUserDialog by mutableStateOf(false)
        private set

    var newFirstname by mutableStateOf("")
    var newLastname by mutableStateOf("")
    var newEmail by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var newRole by mutableStateOf("USER")

    // Reset Password Dialog State
    var showResetPasswordDialog by mutableStateOf(false)
        private set
    var selectedUserForReset by mutableStateOf<UserDto?>(null)
        private set
    var resetNewPassword by mutableStateOf("")

    fun initSession(sessionManager: SessionManager) {
        currentUser = sessionManager.getUser()
        refreshCurrentUser(sessionManager)
    }

    fun refreshCurrentUser(sessionManager: SessionManager) {
        val token = sessionManager.getToken() ?: return
        loadReports(sessionManager)
        viewModelScope.launch {
            try {
                val response = ApiClient.getAuthApi().getCurrentUser("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val updatedUser = response.body()!!
                    currentUser = updatedUser
                    sessionManager.saveSession(token, updatedUser)
                }
            } catch (e: Exception) {
                // Ignore network error on silent refresh, fallback to cached session user
            }

            if (currentUser?.role == "ADMIN") {
                loadUsers(sessionManager)
            }
        }
    }

    fun loadUsers(sessionManager: SessionManager) {
        val token = sessionManager.getToken() ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.getAuthApi().getAllUsers("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    usersList = response.body()!!
                }
            } catch (e: Exception) {
                // Ignore silent list fetch error
            }
        }
    }

    fun loadReports(sessionManager: SessionManager) {
        val token = sessionManager.getToken() ?: return
        isLoadingReports = true
        viewModelScope.launch {
            try {
                val response = ApiClient.getReportsApi().getReports("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    reports = response.body()!!
                    reportsError = null
                } else {
                    reportsError = "Kunde inte hämta rapporter"
                }
            } catch (e: Exception) {
                reportsError = "Kunde inte ansluta till servern"
            }
            isLoadingReports = false
        }
    }

    fun openReport(report: ReportDto) {
        selectedReport = report
        reportStatusError = null
    }

    fun closeReport() {
        selectedReport = null
    }

    // Admin only: the backend rejects status changes from other roles
    fun updateReportStatus(sessionManager: SessionManager, report: ReportDto, status: String) {
        val token = sessionManager.getToken() ?: return
        if (status == report.status || isUpdatingReportStatus) return

        isUpdatingReportStatus = true
        reportStatusError = null
        viewModelScope.launch {
            try {
                val response = ApiClient.getReportsApi()
                    .updateReportStatus("Bearer $token", report.id, UpdateReportStatusRequest(status))
                val updated = response.body()
                if (response.isSuccessful && updated != null) {
                    reports = reports.map { if (it.id == updated.id) updated else it }
                    if (selectedReport?.id == updated.id) selectedReport = updated
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    reportStatusError = parsed ?: "Kunde inte ändra status"
                }
            } catch (e: Exception) {
                reportStatusError = "Kunde inte ansluta till servern"
            }
            isUpdatingReportStatus = false
        }
    }

    fun openCreateUserDialog() {
        newFirstname = ""
        newLastname = ""
        newEmail = ""
        newPassword = ""
        newRole = "USER"
        showCreateUserDialog = true
    }

    fun dismissCreateUserDialog() {
        showCreateUserDialog = false
    }

    fun createUser(sessionManager: SessionManager) {
        val token = sessionManager.getToken() ?: return
        if (newFirstname.isBlank() || newLastname.isBlank() || newEmail.isBlank() || newPassword.isBlank()) {
            statusMessage = "All fields are required to create a user"
            isErrorStatus = true
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val request = CreateUserRequest(
                    firstname = newFirstname.trim(),
                    lastname = newLastname.trim(),
                    email = newEmail.trim(),
                    password = newPassword,
                    role = newRole
                )
                val response = ApiClient.getAuthApi().createUser("Bearer $token", request)
                isLoading = false
                if (response.isSuccessful) {
                    showCreateUserDialog = false
                    statusMessage = "Successfully created user ${newEmail.trim()}"
                    isErrorStatus = false
                    loadUsers(sessionManager)
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: "Failed to create user"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Error connecting to server"
                isErrorStatus = true
            }
        }
    }

    fun openResetPasswordDialog(user: UserDto) {
        selectedUserForReset = user
        resetNewPassword = ""
        showResetPasswordDialog = true
    }

    fun dismissResetPasswordDialog() {
        showResetPasswordDialog = false
        selectedUserForReset = null
    }

    fun resetUserPassword(sessionManager: SessionManager) {
        val user = selectedUserForReset ?: return
        val token = sessionManager.getToken() ?: return
        if (resetNewPassword.length < 8) {
            statusMessage = "New password must be at least 8 characters"
            isErrorStatus = true
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val response = ApiClient.getAuthApi().resetPassword("Bearer $token", user.id, ResetPasswordRequest(resetNewPassword))
                isLoading = false
                if (response.isSuccessful) {
                    showResetPasswordDialog = false
                    statusMessage = "Password reset for ${user.email}"
                    isErrorStatus = false
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: "Failed to reset password"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Error connecting to server"
                isErrorStatus = true
            }
        }
    }

    fun logout(sessionManager: SessionManager, onLoggedOut: () -> Unit) {
        // The ViewModel outlives the session, so drop the previous user's reports
        reports = emptyList()
        selectedReport = null
        ReportImageRepository.clear()
        val token = sessionManager.getToken()
        if (token != null) {
            viewModelScope.launch {
                try {
                    ApiClient.getAuthApi().logout("Bearer $token")
                } catch (e: Exception) {
                    // Silent catch on network error during logout
                } finally {
                    sessionManager.clearSession()
                    onLoggedOut()
                }
            }
        } else {
            sessionManager.clearSession()
            onLoggedOut()
        }
    }
}
