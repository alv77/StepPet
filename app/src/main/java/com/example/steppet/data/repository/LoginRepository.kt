package com.example.steppet.data.repository

import android.content.Context
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.local.UserEntity

class LoginRepository(context: Context) {
    private val userDao = AppDatabase.getInstance(context).userDao()

    /** Saves or updates the single UserEntity row. */
    suspend fun saveCredentials(username: String, password: String) {
        userDao.upsert(UserEntity(username = username, password = password))
    }

    /** Clears that row entirely (logs out). */
    suspend fun clearCredentials() {
        userDao.clearAll()
    }

    /** Returns the saved username, or null. */
    suspend fun getUsername(): String? =
        userDao.getUser()?.username

    /** Returns the saved password, or null. */
    suspend fun getPassword(): String? =
        userDao.getUser()?.password
}
