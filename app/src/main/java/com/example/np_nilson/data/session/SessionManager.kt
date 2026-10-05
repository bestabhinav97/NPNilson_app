package com.example.np_nilson.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.np_nilson.data.api.UserDto

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        EncryptedSharedPreferences.create(
            "np_nilson_secure_session",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.values()[0],
            EncryptedSharedPreferences.PrefValueEncryptionScheme.values()[0]
        )
    } catch (e: Exception) {
        context.getSharedPreferences("np_nilson_session_fallback", Context.MODE_PRIVATE)
    }

    fun saveSession(token: String, user: UserDto) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, user.id)
            putString(KEY_FIRSTNAME, user.firstname)
            putString(KEY_LASTNAME, user.lastname)
            putString(KEY_EMAIL, user.email)
            putString(KEY_ROLE, user.role)
            apply()
        }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUser(): UserDto? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val firstname = prefs.getString(KEY_FIRSTNAME, "") ?: ""
        val lastname = prefs.getString(KEY_LASTNAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val role = prefs.getString(KEY_ROLE, "USER") ?: "USER"
        return UserDto(id = id, firstname = firstname, lastname = lastname, email = email, role = role)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    companion object {
        private const val KEY_TOKEN = "key_session_token"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_FIRSTNAME = "key_firstname"
        private const val KEY_LASTNAME = "key_lastname"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_ROLE = "key_role"
    }
}
