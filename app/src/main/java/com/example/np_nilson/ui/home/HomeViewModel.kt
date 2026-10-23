package com.example.np_nilson.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.np_nilson.data.api.AdminDashboardDto
import com.example.np_nilson.data.api.ApiClient
import com.example.np_nilson.data.api.CreateStoreRequest
import com.example.np_nilson.data.api.CreateUserRequest
import com.example.np_nilson.data.api.ErrorResponse
import com.example.np_nilson.data.api.ReportDto
import com.example.np_nilson.data.api.ResetPasswordRequest
import com.example.np_nilson.data.api.SetUserActiveRequest
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

    var adminDashboard by mutableStateOf<AdminDashboardDto?>(null)
        private set

    var isLoadingAdminDashboard by mutableStateOf(false)
        private set

    var userSearchQuery by mutableStateOf("")

    val filteredUsersList: List<UserDto>
        get() {
            if (userSearchQuery.isBlank()) {
                return usersList
            }

            val query = userSearchQuery
                .trim()
                .lowercase()

            return usersList.filter { user ->
                "${user.firstname} ${user.lastname}"
                    .lowercase()
                    .contains(query) ||
                        user.email
                            .lowercase()
                            .contains(query) ||
                        user.role
                            .lowercase()
                            .contains(query) ||
                        (
                                user.store
                                    ?.storeName
                                    ?.lowercase()
                                    ?.contains(query)
                                        == true
                                )
            }
        }

    var isLoading by mutableStateOf(false)
        private set

    // ---------------------------------------------------------
    // Reports
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // General admin status
    // ---------------------------------------------------------

    var statusMessage by mutableStateOf<String?>(null)
        private set

    var isErrorStatus by mutableStateOf(false)
        private set

    // ---------------------------------------------------------
    // Create user state
    // ---------------------------------------------------------

    var showCreateUserDialog by mutableStateOf(false)
        private set

    var newFirstname by mutableStateOf("")
    var newLastname by mutableStateOf("")
    var newEmail by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var newRole by mutableStateOf("USER")
    var newStoreId by mutableStateOf<Long?>(null)

    // ---------------------------------------------------------
    // Edit user state
    // ---------------------------------------------------------

    var showEditUserDialog by mutableStateOf(false)
        private set

    var editingUser by mutableStateOf<UserDto?>(null)
        private set

    var editFirstname by mutableStateOf("")
    var editLastname by mutableStateOf("")
    var editEmail by mutableStateOf("")
    var editRole by mutableStateOf("USER")
    var editStoreId by mutableStateOf<Long?>(null)

    // ---------------------------------------------------------
    // Delete user state
    // ---------------------------------------------------------

    var showDeleteUserDialog by mutableStateOf(false)
        private set

    var userToDelete by mutableStateOf<UserDto?>(null)
        private set

    // ---------------------------------------------------------
    // Reset password state
    // ---------------------------------------------------------

    var showResetPasswordDialog by mutableStateOf(false)
        private set

    var selectedUserForReset by mutableStateOf<UserDto?>(null)
        private set

    var resetNewPassword by mutableStateOf("")

    // ---------------------------------------------------------
    // Store CRUD state
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // Session
    // ---------------------------------------------------------

    fun initSession(
        sessionManager: SessionManager
    ) {
        currentUser =
            sessionManager.getUser()

        refreshCurrentUser(
            sessionManager
        )
    }

    fun refreshCurrentUser(
        sessionManager: SessionManager
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        loadReports(
            sessionManager
        )

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getAuthApi()
                        .getCurrentUser(
                            "Bearer $token"
                        )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {
                    val updatedUser =
                        response.body()!!

                    currentUser =
                        updatedUser

                    sessionManager.saveSession(
                        token,
                        updatedUser
                    )
                }
            } catch (_: Exception) {
                // Use cached session user if silent refresh fails.
            }

            if (
                currentUser?.role ==
                "ADMIN"
            ) {
                loadUsers(
                    sessionManager
                )

                loadStores(
                    sessionManager
                )

                loadAdminDashboard(
                    sessionManager
                )
            }
        }
    }

    // ---------------------------------------------------------
    // Admin dashboard
    // ---------------------------------------------------------

    fun loadAdminDashboard(
        sessionManager: SessionManager
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        isLoadingAdminDashboard =
            true

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getAuthApi()
                        .getAdminDashboard(
                            "Bearer $token"
                        )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {
                    adminDashboard =
                        response.body()
                }
            } catch (_: Exception) {
                // Dashboard remains on its last successfully loaded value.
            } finally {
                isLoadingAdminDashboard =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Users
    // ---------------------------------------------------------

    fun loadUsers(
        sessionManager: SessionManager
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getAuthApi()
                        .getAllUsers(
                            "Bearer $token"
                        )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {
                    usersList =
                        response.body()!!
                }
            } catch (_: Exception) {
                // Silent list fetch failure.
            }
        }
    }

    // ---------------------------------------------------------
    // Stores
    // ---------------------------------------------------------

    fun loadStores(
        sessionManager: SessionManager
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getAuthApi()
                        .getStores(
                            "Bearer $token"
                        )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {
                    storesList =
                        response.body()!!
                }
            } catch (_: Exception) {
                // Silent list fetch failure.
            }
        }
    }

    // ---------------------------------------------------------
    // Reports
    // ---------------------------------------------------------

    fun loadReports(
        sessionManager: SessionManager
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        isLoadingReports =
            true

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getReportsApi()
                        .getReports(
                            "Bearer $token"
                        )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {
                    reports =
                        response.body()!!

                    reportsError =
                        null
                } else {
                    reportsError =
                        "Kunde inte hämta rapporter"
                }
            } catch (_: Exception) {
                reportsError =
                    "Kunde inte ansluta till servern"
            } finally {
                isLoadingReports =
                    false
            }
        }
    }

    fun openReport(
        report: ReportDto
    ) {
        selectedReport =
            report

        reportStatusError =
            null
    }

    fun closeReport() {
        selectedReport =
            null
    }

    fun updateReportStatus(
        sessionManager: SessionManager,
        report: ReportDto,
        status: String
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        if (
            status == report.status ||
            isUpdatingReportStatus
        ) {
            return
        }

        isUpdatingReportStatus =
            true

        reportStatusError =
            null

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getReportsApi()
                        .updateReportStatus(
                            "Bearer $token",
                            report.id,
                            UpdateReportStatusRequest(
                                status
                            )
                        )

                val updated =
                    response.body()

                if (
                    response.isSuccessful &&
                    updated != null
                ) {
                    reports =
                        reports.map {
                                existing ->

                            if (
                                existing.id ==
                                updated.id
                            ) {
                                updated
                            } else {
                                existing
                            }
                        }

                    if (
                        selectedReport?.id ==
                        updated.id
                    ) {
                        selectedReport =
                            updated
                    }

                    if (
                        currentUser?.role ==
                        "ADMIN"
                    ) {
                        loadAdminDashboard(
                            sessionManager
                        )
                    }
                } else {
                    val errorStr =
                        response
                            .errorBody()
                            ?.string()

                    val parsed =
                        parseApiError(
                            errorStr
                        )

                    reportStatusError =
                        parsed
                            ?: "Kunde inte ändra status"
                }
            } catch (_: Exception) {
                reportStatusError =
                    "Kunde inte ansluta till servern"
            } finally {
                isUpdatingReportStatus =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Create user
    // ---------------------------------------------------------

    fun openCreateUserDialog() {
        newFirstname = ""
        newLastname = ""
        newEmail = ""
        newPassword = ""
        newRole = "USER"

        newStoreId =
            storesList
                .firstOrNull()
                ?.storeId

        showCreateUserDialog =
            true
    }

    fun dismissCreateUserDialog() {
        showCreateUserDialog =
            false
    }

    fun createUser(
        sessionManager: SessionManager
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        if (
            newFirstname.isBlank() ||
            newLastname.isBlank() ||
            newEmail.isBlank() ||
            newPassword.isBlank()
        ) {
            statusMessage =
                "Alla fält krävs för att skapa en användare"

            isErrorStatus =
                true

            return
        }

        isLoading =
            true

        viewModelScope.launch {
            try {
                val request =
                    CreateUserRequest(
                        firstname =
                            newFirstname.trim(),

                        lastname =
                            newLastname.trim(),

                        email =
                            newEmail.trim(),

                        password =
                            newPassword,

                        role =
                            newRole,

                        storeId =
                            newStoreId
                    )

                val response =
                    ApiClient
                        .getAuthApi()
                        .createUser(
                            "Bearer $token",
                            request
                        )

                if (
                    response.isSuccessful
                ) {
                    showCreateUserDialog =
                        false

                    statusMessage =
                        "Användare ${newEmail.trim()} skapades"

                    isErrorStatus =
                        false

                    loadUsers(
                        sessionManager
                    )

                    loadAdminDashboard(
                        sessionManager
                    )
                } else {
                    val parsed =
                        parseApiError(
                            response
                                .errorBody()
                                ?.string()
                        )

                    statusMessage =
                        parsed
                            ?: "Kunde inte skapa användare"

                    isErrorStatus =
                        true
                }
            } catch (_: Exception) {
                statusMessage =
                    "Kunde inte ansluta till servern"

                isErrorStatus =
                    true
            } finally {
                isLoading =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Edit user
    // ---------------------------------------------------------

    fun openEditUserDialog(
        user: UserDto
    ) {
        editingUser =
            user

        editFirstname =
            user.firstname

        editLastname =
            user.lastname

        editEmail =
            user.email

        editRole =
            user.role

        editStoreId =
            user.store?.storeId

        showEditUserDialog =
            true
    }

    fun dismissEditUserDialog() {
        showEditUserDialog =
            false

        editingUser =
            null
    }

    fun updateUser(
        sessionManager: SessionManager
    ) {
        val user =
            editingUser
                ?: return

        val token =
            sessionManager.getToken()
                ?: return

        if (
            editFirstname.isBlank() ||
            editLastname.isBlank() ||
            editEmail.isBlank()
        ) {
            statusMessage =
                "Förnamn, efternamn och e-post krävs"

            isErrorStatus =
                true

            return
        }

        isLoading =
            true

        viewModelScope.launch {
            try {
                val request =
                    UpdateUserRequest(
                        firstname =
                            editFirstname.trim(),

                        lastname =
                            editLastname.trim(),

                        email =
                            editEmail.trim(),

                        role =
                            editRole,

                        storeId =
                            editStoreId
                    )

                val response =
                    ApiClient
                        .getAuthApi()
                        .updateUser(
                            "Bearer $token",
                            user.id,
                            request
                        )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {
                    val updatedUser =
                        response.body()!!

                    showEditUserDialog =
                        false

                    editingUser =
                        null

                    statusMessage =
                        "Uppdaterade användaren ${updatedUser.email}"

                    isErrorStatus =
                        false

                    loadUsers(
                        sessionManager
                    )

                    loadAdminDashboard(
                        sessionManager
                    )

                    if (
                        currentUser?.id ==
                        user.id
                    ) {
                        currentUser =
                            updatedUser

                        sessionManager.saveSession(
                            token,
                            updatedUser
                        )
                    }
                } else {
                    val errorStr =
                        response
                            .errorBody()
                            ?.string()

                    val parsed =
                        parseApiError(
                            errorStr
                        )

                    statusMessage =
                        parsed
                            ?: errorStr
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                    ?: "Kunde inte uppdatera användare"

                    isErrorStatus =
                        true
                }
            } catch (e: Exception) {
                statusMessage =
                    "Kunde inte ansluta till servern: ${e.localizedMessage}"

                isErrorStatus =
                    true
            } finally {
                isLoading =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Activate / deactivate user
    // ---------------------------------------------------------

    fun setUserActive(
        sessionManager: SessionManager,
        user: UserDto,
        active: Boolean
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        if (isLoading) {
            return
        }

        isLoading =
            true

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getAuthApi()
                        .setUserActive(
                            "Bearer $token",
                            user.id,
                            SetUserActiveRequest(
                                active = active
                            )
                        )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {
                    val updatedUser =
                        response.body()!!

                    usersList =
                        usersList.map {
                                existing ->

                            if (
                                existing.id ==
                                updatedUser.id
                            ) {
                                updatedUser
                            } else {
                                existing
                            }
                        }

                    statusMessage =
                        if (active) {
                            "Aktiverade ${updatedUser.email}"
                        } else {
                            "Inaktiverade ${updatedUser.email}"
                        }

                    isErrorStatus =
                        false

                    loadUsers(
                        sessionManager
                    )

                    loadAdminDashboard(
                        sessionManager
                    )
                } else {
                    val parsed =
                        parseApiError(
                            response
                                .errorBody()
                                ?.string()
                        )

                    statusMessage =
                        parsed
                            ?: "Kunde inte ändra kontostatus"

                    isErrorStatus =
                        true
                }
            } catch (_: Exception) {
                statusMessage =
                    "Kunde inte ansluta till servern"

                isErrorStatus =
                    true
            } finally {
                isLoading =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Delete user
    // ---------------------------------------------------------

    fun openDeleteUserDialog(
        user: UserDto
    ) {
        userToDelete =
            user

        showDeleteUserDialog =
            true
    }

    fun dismissDeleteUserDialog() {
        showDeleteUserDialog =
            false

        userToDelete =
            null
    }

    fun deleteUser(
        sessionManager: SessionManager
    ) {
        val user =
            userToDelete
                ?: return

        val token =
            sessionManager.getToken()
                ?: return

        isLoading =
            true

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getAuthApi()
                        .deleteUser(
                            "Bearer $token",
                            user.id
                        )

                if (
                    response.isSuccessful
                ) {
                    showDeleteUserDialog =
                        false

                    userToDelete =
                        null

                    statusMessage =
                        "Tog bort användare ${user.email}"

                    isErrorStatus =
                        false

                    loadUsers(
                        sessionManager
                    )

                    loadAdminDashboard(
                        sessionManager
                    )
                } else {
                    val parsed =
                        parseApiError(
                            response
                                .errorBody()
                                ?.string()
                        )

                    statusMessage =
                        parsed
                            ?: "Kunde inte ta bort användare"

                    isErrorStatus =
                        true
                }
            } catch (_: Exception) {
                statusMessage =
                    "Kunde inte ansluta till servern"

                isErrorStatus =
                    true
            } finally {
                isLoading =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Reset user password
    // ---------------------------------------------------------

    fun openResetPasswordDialog(
        user: UserDto
    ) {
        selectedUserForReset =
            user

        resetNewPassword =
            ""

        showResetPasswordDialog =
            true
    }

    fun dismissResetPasswordDialog() {
        showResetPasswordDialog =
            false

        selectedUserForReset =
            null
    }

    fun resetUserPassword(
        sessionManager: SessionManager
    ) {
        val user =
            selectedUserForReset
                ?: return

        val token =
            sessionManager.getToken()
                ?: return

        if (
            resetNewPassword.length <
            8
        ) {
            statusMessage =
                "Nytt lösenord måste vara minst 8 tecken"

            isErrorStatus =
                true

            return
        }

        isLoading =
            true

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getAuthApi()
                        .resetPassword(
                            "Bearer $token",
                            user.id,
                            ResetPasswordRequest(
                                resetNewPassword
                            )
                        )

                if (
                    response.isSuccessful
                ) {
                    showResetPasswordDialog =
                        false

                    selectedUserForReset =
                        null

                    resetNewPassword =
                        ""

                    statusMessage =
                        "Lösenord återställt för ${user.email}"

                    isErrorStatus =
                        false

                    loadAdminDashboard(
                        sessionManager
                    )
                } else {
                    val parsed =
                        parseApiError(
                            response
                                .errorBody()
                                ?.string()
                        )

                    statusMessage =
                        parsed
                            ?: "Kunde inte återställa lösenord"

                    isErrorStatus =
                        true
                }
            } catch (_: Exception) {
                statusMessage =
                    "Kunde inte ansluta till servern"

                isErrorStatus =
                    true
            } finally {
                isLoading =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Create store
    // ---------------------------------------------------------

    fun openCreateStoreDialog() {
        newStoreName =
            ""

        newStoreAddress =
            ""

        showCreateStoreDialog =
            true
    }

    fun dismissCreateStoreDialog() {
        showCreateStoreDialog =
            false
    }

    fun createStore(
        sessionManager: SessionManager
    ) {
        val token =
            sessionManager.getToken()
                ?: return

        if (
            newStoreName.isBlank() ||
            newStoreAddress.isBlank()
        ) {
            statusMessage =
                "Butiksnamn och adress krävs"

            isErrorStatus =
                true

            return
        }

        isLoading =
            true

        viewModelScope.launch {
            try {
                val request =
                    CreateStoreRequest(
                        storeName =
                            newStoreName.trim(),

                        address =
                            newStoreAddress.trim()
                    )

                val response =
                    ApiClient
                        .getAuthApi()
                        .createStore(
                            "Bearer $token",
                            request
                        )

                if (
                    response.isSuccessful
                ) {
                    showCreateStoreDialog =
                        false

                    statusMessage =
                        "Skapade butiken ${newStoreName.trim()}"

                    isErrorStatus =
                        false

                    loadStores(
                        sessionManager
                    )

                    loadAdminDashboard(
                        sessionManager
                    )
                } else {
                    val parsed =
                        parseApiError(
                            response
                                .errorBody()
                                ?.string()
                        )

                    statusMessage =
                        parsed
                            ?: "Kunde inte skapa butik"

                    isErrorStatus =
                        true
                }
            } catch (_: Exception) {
                statusMessage =
                    "Kunde inte ansluta till servern"

                isErrorStatus =
                    true
            } finally {
                isLoading =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Edit store
    // ---------------------------------------------------------

    fun openEditStoreDialog(
        store: StoreDto
    ) {
        editingStore =
            store

        editStoreName =
            store.storeName

        editStoreAddress =
            store.address

        showEditStoreDialog =
            true
    }

    fun dismissEditStoreDialog() {
        showEditStoreDialog =
            false

        editingStore =
            null
    }

    fun updateStore(
        sessionManager: SessionManager
    ) {
        val store =
            editingStore
                ?: return

        val token =
            sessionManager.getToken()
                ?: return

        if (
            editStoreName.isBlank() ||
            editStoreAddress.isBlank()
        ) {
            statusMessage =
                "Butiksnamn och adress krävs"

            isErrorStatus =
                true

            return
        }

        isLoading =
            true

        viewModelScope.launch {
            try {
                val request =
                    UpdateStoreRequest(
                        storeName =
                            editStoreName.trim(),

                        address =
                            editStoreAddress.trim()
                    )

                val response =
                    ApiClient
                        .getAuthApi()
                        .updateStore(
                            "Bearer $token",
                            store.storeId,
                            request
                        )

                if (
                    response.isSuccessful
                ) {
                    showEditStoreDialog =
                        false

                    editingStore =
                        null

                    statusMessage =
                        "Uppdaterade butiken ${editStoreName.trim()}"

                    isErrorStatus =
                        false

                    loadStores(
                        sessionManager
                    )

                    loadUsers(
                        sessionManager
                    )

                    loadAdminDashboard(
                        sessionManager
                    )
                } else {
                    val parsed =
                        parseApiError(
                            response
                                .errorBody()
                                ?.string()
                        )

                    statusMessage =
                        parsed
                            ?: "Kunde inte uppdatera butik"

                    isErrorStatus =
                        true
                }
            } catch (_: Exception) {
                statusMessage =
                    "Kunde inte ansluta till servern"

                isErrorStatus =
                    true
            } finally {
                isLoading =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Delete store
    // ---------------------------------------------------------

    fun openDeleteStoreDialog(
        store: StoreDto
    ) {
        storeToDelete =
            store

        showDeleteStoreDialog =
            true
    }

    fun dismissDeleteStoreDialog() {
        showDeleteStoreDialog =
            false

        storeToDelete =
            null
    }

    fun deleteStore(
        sessionManager: SessionManager
    ) {
        val store =
            storeToDelete
                ?: return

        val token =
            sessionManager.getToken()
                ?: return

        isLoading =
            true

        viewModelScope.launch {
            try {
                val response =
                    ApiClient
                        .getAuthApi()
                        .deleteStore(
                            "Bearer $token",
                            store.storeId
                        )

                if (
                    response.isSuccessful
                ) {
                    showDeleteStoreDialog =
                        false

                    storeToDelete =
                        null

                    statusMessage =
                        "Tog bort butik ${store.storeName}"

                    isErrorStatus =
                        false

                    loadStores(
                        sessionManager
                    )

                    loadUsers(
                        sessionManager
                    )

                    loadAdminDashboard(
                        sessionManager
                    )
                } else {
                    val parsed =
                        parseApiError(
                            response
                                .errorBody()
                                ?.string()
                        )

                    statusMessage =
                        parsed
                            ?: "Kunde inte ta bort butik"

                    isErrorStatus =
                        true
                }
            } catch (_: Exception) {
                statusMessage =
                    "Kunde inte ansluta till servern"

                isErrorStatus =
                    true
            } finally {
                isLoading =
                    false
            }
        }
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun parseApiError(
        errorBody: String?
    ): String? {
        if (
            errorBody.isNullOrBlank()
        ) {
            return null
        }

        return try {
            Gson()
                .fromJson(
                    errorBody,
                    ErrorResponse::class.java
                )
                ?.error
        } catch (_: Exception) {
            null
        }
    }

    // ---------------------------------------------------------
    // Logout
    // ---------------------------------------------------------

    fun logout(
        sessionManager: SessionManager,
        onLoggedOut: () -> Unit
    ) {
        reports =
            emptyList()

        selectedReport =
            null

        usersList =
            emptyList()

        storesList =
            emptyList()

        adminDashboard =
            null

        statusMessage =
            null

        ReportImageRepository.clear()

        val token =
            sessionManager.getToken()

        if (
            token != null
        ) {
            viewModelScope.launch {
                try {
                    ApiClient
                        .getAuthApi()
                        .logout(
                            "Bearer $token"
                        )
                } catch (_: Exception) {
                    // Logout locally even if server request fails.
                } finally {
                    sessionManager
                        .clearSession()

                    onLoggedOut()
                }
            }
        } else {
            sessionManager
                .clearSession()

            onLoggedOut()
        }
    }
}