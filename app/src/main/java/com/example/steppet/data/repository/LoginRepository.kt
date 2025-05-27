package com.example.steppet.data.repository

import android.content.Context
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.local.UserEntity

class LoginRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).userDao()

    /** Registers a new user. Returns true on success, false on conflict (duplicate username). */
    suspend fun register(username: String, password: String): Boolean {
        return try {
            dao.insert(UserEntity(username = username, password = password))
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Returns true if a user with that exact username+password exists. */
    suspend fun login(username: String, password: String): Boolean {
        return dao.findUser(username, password) != null
    }
}
