package com.example.steppet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.steppet.data.repository.LoginRepository
import androidx.compose.runtime.*

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = LoginRepository(application)

    var username by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var loginSuccess by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onUsernameChange(new: String) { username = new }
    fun onPasswordChange(new: String) { password = new }

    fun register(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || password.isBlank()) {
                errorMessage = "Username & password required"
                onResult(false)
            } else {
                val ok = repo.register(username, password)
                if (ok) {
                    errorMessage = null
                    loginSuccess = true
                    onResult(true)
                } else {
                    errorMessage = "Username already exists"
                    onResult(false)
                }
            }
        }
    }

    fun login(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.login(username, password)
            if (ok) {
                errorMessage = null
                loginSuccess = true
                onResult(true)
            } else {
                errorMessage = "Invalid credentials"
                onResult(false)
            }
        }
    }

    fun logout() {
        username = ""
        password = ""
        errorMessage = null
        loginSuccess = false
    }

    fun changeUsername(newName: String) {
        val old = username
        viewModelScope.launch {
            if (repo.changeUsername(old, newName)) {
                username = newName
            }
        }
    }

    fun deleteAccount(onResult: () -> Unit) {
        val user = username
        viewModelScope.launch {
            repo.deleteUser(user)
            logout()
            onResult()
        }
    }

}
