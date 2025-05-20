package com.example.steppet.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Stores and retrieves login credentials securely using EncryptedSharedPreferences.
 */
class LoginRepository(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "login_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }

    /** Saves the provided username and password. */
    fun saveCredentials(username: String, password: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    /** Clears all stored credentials. */
    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    /** Retrieves the stored username, or null if none. */
    fun getUsername(): String? =
        prefs.getString(KEY_USERNAME, null)

    /** Retrieves the stored password, or null if none. */
    fun getPassword(): String? =
        prefs.getString(KEY_PASSWORD, null)
}
