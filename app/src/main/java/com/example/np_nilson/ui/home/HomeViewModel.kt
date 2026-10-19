package com.example.np_nilson.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.np_nilson.data.api.ApiClient
import com.example.np_nilson.data.api.CreateStoreRequest
import com.example.np_nilson.data.api.CreateUserRequest
import com.example.np_nilson.data.api.ErrorResponse
import com.example.np_nilson.data.api.ReportDto
import com.example.np_nilson.data.api.ResetPasswordRequest
import com.example.np_nilson.data.api.StoreDto
import com.example.np_nilson.data.api.UpdateReportStatusRequest
import com.example.np_nilson.data.api.UpdateStoreRequest
import com.example.np_nilson.data.api.UpdateUserRequest
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

    var storesList by mutableStateOf<List<StoreDto>>(emptyList())
        private set

    var userSearchQuery by mutableStateOf("")

    val filteredUsersList: List<UserDto>
        get() {
            if (userSearchQuery.isBlank()) return usersList
            val query = userSearchQuery.trim().lowercase()
            return usersList.filter { u ->
                "${u.firstname} ${u.lastname}".lowercase().contains(query) ||
                        u.email.lowercase().contains(query) ||
                        u.role.lowercase().contains(query) ||
                        (u.store?.storeName?.lowercase()?.contains(query) == true)
            }
        }

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
    var newStoreId by mutableStateOf<Long?>(null)

    // Edit User Dialog State
    var showEditUserDialog by mutableStateOf(false)
        private set
    var editingUser by mutableStateOf<UserDto?>(null)
        private set
    var editFirstname by mutableStateOf("")
    var editLastname by mutableStateOf("")
    var editEmail by mutableStateOf("")
    var editRole by mutableStateOf("USER")
    var editStoreId by mutableStateOf<Long?>(null)

    // Delete User Dialog State
    var showDeleteUserDialog by mutableStateOf(false)
        private set
    var userToDelete by mutableStateOf<UserDto?>(null)
        private set

    // Reset Password Dialog State
    var showResetPasswordDialog by mutableStateOf(false)
        private set
    var selectedUserForReset by mutableStateOf<UserDto?>(null)
        private set
    var resetNewPassword by mutableStateOf("")

    // Store CRUD States
    var showCreateStoreDialog by mutableStateOf(false)
        private set
    var newStoreName by mutableStateOf("")
    var newStoreAddress by mutableStateOf("")

    var showEditStoreDialog by mutableStateOf(false)
        private set
    var editingStore by mutableStateOf<StoreDto?>(null)
        private set
    var editStoreName by mutableStateOf("")
    var editStoreAddress by mutableStateOf("")

    var showDeleteStoreDialog by mutableStateOf(false)
        private set
    var storeToDelete by mutableStateOf<StoreDto?>(null)
        private set

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
                loadStores(sessionManager)
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

    fun loadStores(sessionManager: SessionManager) {
        val token = sessionManager.getToken() ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.getAuthApi().getStores("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    storesList = response.body()!!
                }
            } catch (e: Exception) {
                // Ignore silent fetch error
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
        newStoreId = storesList.firstOrNull()?.storeId
        showCreateUserDialog = true
    }

    fun dismissCreateUserDialog() {
        showCreateUserDialog = false
    }

    fun createUser(sessionManager: SessionManager) {
        val token = sessionManager.getToken() ?: return
        if (newFirstname.isBlank() || newLastname.isBlank() || newEmail.isBlank() || newPassword.isBlank()) {
            statusMessage = "Alla fält krävs för att skapa en användare"
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
                    role = newRole,
                    storeId = newStoreId
                )
                val response = ApiClient.getAuthApi().createUser("Bearer $token", request)
                isLoading = false
                if (response.isSuccessful) {
                    showCreateUserDialog = false
                    statusMessage = "Användare ${newEmail.trim()} skapades"
                    isErrorStatus = false
                    loadUsers(sessionManager)
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: "Kunde inte skapa användare"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Kunde inte ansluta till servern"
                isErrorStatus = true
            }
        }
    }

    fun openEditUserDialog(user: UserDto) {
        editingUser = user
        editFirstname = user.firstname
        editLastname = user.lastname
        editEmail = user.email
        editRole = user.role
        editStoreId = user.store?.storeId
        showEditUserDialog = true
    }

    fun dismissEditUserDialog() {
        showEditUserDialog = false
        editingUser = null
    }

    fun updateUser(sessionManager: SessionManager) {
        val user = editingUser ?: return
        val token = sessionManager.getToken() ?: return
        if (editFirstname.isBlank() || editLastname.isBlank() || editEmail.isBlank()) {
            statusMessage = "Förnamn, efternamn och e-post krävs"
            isErrorStatus = true
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val request = UpdateUserRequest(
                    firstname = editFirstname.trim(),
                    lastname = editLastname.trim(),
                    email = editEmail.trim(),
                    role = editRole,
                    storeId = editStoreId
                )
                val response = ApiClient.getAuthApi().updateUser("Bearer $token", user.id, request)
                isLoading = false
                if (response.isSuccessful && response.body() != null) {
                    val updatedUser = response.body()!!
                    showEditUserDialog = false
                    statusMessage = "Uppdaterade användaren ${updatedUser.email}"
                    isErrorStatus = false
                    loadUsers(sessionManager)
                    if (currentUser?.id == user.id) {
                        currentUser = updatedUser
                    }
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: errorStr?.takeIf { it.isNotBlank() } ?: "Kunde inte uppdatera användare"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Kunde inte ansluta till servern: ${e.localizedMessage}"
                isErrorStatus = true
            }
        }
    }

    fun openDeleteUserDialog(user: UserDto) {
        userToDelete = user
        showDeleteUserDialog = true
    }

    fun dismissDeleteUserDialog() {
        showDeleteUserDialog = false
        userToDelete = null
    }

    fun deleteUser(sessionManager: SessionManager) {
        val user = userToDelete ?: return
        val token = sessionManager.getToken() ?: return
        isLoading = true
        viewModelScope.launch {
            try {
                val response = ApiClient.getAuthApi().deleteUser("Bearer $token", user.id)
                isLoading = false
                if (response.isSuccessful) {
                    showDeleteUserDialog = false
                    statusMessage = "Tog bort användare ${user.email}"
                    isErrorStatus = false
                    loadUsers(sessionManager)
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: "Kunde inte ta bort användare"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Kunde inte ansluta till servern"
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
            statusMessage = "Nytt lösenord måste vara minst 8 tecken"
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
                    statusMessage = "Lösenord återställt för ${user.email}"
                    isErrorStatus = false
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: "Kunde inte återställa lösenord"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Kunde inte ansluta till servern"
                isErrorStatus = true
            }
        }
    }

    fun openCreateStoreDialog() {
        newStoreName = ""
        newStoreAddress = ""
        showCreateStoreDialog = true
    }

    fun dismissCreateStoreDialog() {
        showCreateStoreDialog = false
    }

    fun createStore(sessionManager: SessionManager) {
        val token = sessionManager.getToken() ?: return
        if (newStoreName.isBlank() || newStoreAddress.isBlank()) {
            statusMessage = "Butiksnamn och adress krävs"
            isErrorStatus = true
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val request = CreateStoreRequest(newStoreName.trim(), newStoreAddress.trim())
                val response = ApiClient.getAuthApi().createStore("Bearer $token", request)
                isLoading = false
                if (response.isSuccessful) {
                    showCreateStoreDialog = false
                    statusMessage = "Skapade butiken ${newStoreName.trim()}"
                    isErrorStatus = false
                    loadStores(sessionManager)
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: "Kunde inte skapa butik"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Kunde inte ansluta till servern"
                isErrorStatus = true
            }
        }
    }

    fun openEditStoreDialog(store: StoreDto) {
        editingStore = store
        editStoreName = store.storeName
        editStoreAddress = store.address
        showEditStoreDialog = true
    }

    fun dismissEditStoreDialog() {
        showEditStoreDialog = false
        editingStore = null
    }

    fun updateStore(sessionManager: SessionManager) {
        val store = editingStore ?: return
        val token = sessionManager.getToken() ?: return
        if (editStoreName.isBlank() || editStoreAddress.isBlank()) {
            statusMessage = "Butiksnamn och adress krävs"
            isErrorStatus = true
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val request = UpdateStoreRequest(editStoreName.trim(), editStoreAddress.trim())
                val response = ApiClient.getAuthApi().updateStore("Bearer $token", store.storeId, request)
                isLoading = false
                if (response.isSuccessful) {
                    showEditStoreDialog = false
                    statusMessage = "Uppdaterade butiken ${editStoreName.trim()}"
                    isErrorStatus = false
                    loadStores(sessionManager)
                    loadUsers(sessionManager)
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: "Kunde inte uppdatera butik"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Kunde inte ansluta till servern"
                isErrorStatus = true
            }
        }
    }

    fun openDeleteStoreDialog(store: StoreDto) {
        storeToDelete = store
        showDeleteStoreDialog = true
    }

    fun dismissDeleteStoreDialog() {
        showDeleteStoreDialog = false
        storeToDelete = null
    }

    fun deleteStore(sessionManager: SessionManager) {
        val store = storeToDelete ?: return
        val token = sessionManager.getToken() ?: return
        isLoading = true
        viewModelScope.launch {
            try {
                val response = ApiClient.getAuthApi().deleteStore("Bearer $token", store.storeId)
                isLoading = false
                if (response.isSuccessful) {
                    showDeleteStoreDialog = false
                    statusMessage = "Tog bort butik ${store.storeName}"
                    isErrorStatus = false
                    loadStores(sessionManager)
                    loadUsers(sessionManager)
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    statusMessage = parsed ?: "Kunde inte ta bort butik"
                    isErrorStatus = true
                }
            } catch (e: Exception) {
                isLoading = false
                statusMessage = "Kunde inte ansluta till servern"
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
