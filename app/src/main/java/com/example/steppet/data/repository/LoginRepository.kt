package com.example.steppet.data.repository

import android.content.Context
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.local.UserEntity

class LoginRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).userDao()

    suspend fun register(username: String, password: String): Boolean =
        try {
            dao.insert(UserEntity(username = username, password = password))
            true
        } catch (e: Exception) {
            false
        }

    suspend fun login(username: String, password: String): Boolean =
        dao.findUser(username, password) != null

    suspend fun changeUsername(oldName: String, newName: String): Boolean =
        dao.updateUsername(oldName, newName) > 0

    suspend fun deleteUser(username: String): Boolean =
        dao.deleteUser(username) > 0
}
