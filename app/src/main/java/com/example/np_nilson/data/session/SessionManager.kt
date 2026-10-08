package com.example.np_nilson.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.np_nilson.data.api.StoreDto
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
            if (user.store != null) {
                putLong(KEY_STORE_ID, user.store.storeId)
                putString(KEY_STORE_NAME, user.store.storeName)
                putString(KEY_STORE_ADDRESS, user.store.address)
            } else {
                remove(KEY_STORE_ID)
                remove(KEY_STORE_NAME)
                remove(KEY_STORE_ADDRESS)
            }
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

        val storeId = if (prefs.contains(KEY_STORE_ID)) prefs.getLong(KEY_STORE_ID, -1L) else null
        val storeName = prefs.getString(KEY_STORE_NAME, null)
        val storeAddress = prefs.getString(KEY_STORE_ADDRESS, null)

        val store = if (storeId != null && storeId != -1L && storeName != null && storeAddress != null) {
            StoreDto(storeId = storeId, storeName = storeName, address = storeAddress)
        } else null

        return UserDto(
            id = id,
            firstname = firstname,
            lastname = lastname,
            email = email,
            role = role,
            store = store
        )
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
        private const val KEY_STORE_ID = "key_store_id"
        private const val KEY_STORE_NAME = "key_store_name"
        private const val KEY_STORE_ADDRESS = "key_store_address"
    }
}
