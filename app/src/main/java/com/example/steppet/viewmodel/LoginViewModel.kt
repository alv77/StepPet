package com.example.steppet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.example.steppet.data.repository.LoginRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles user login/logout and secure credential storage via Room.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LoginRepository(application)

    var username by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var loginSuccess by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Auto-login if credentials exist
        viewModelScope.launch {
            val u = repository.getUsername()
            val p = repository.getPassword()
            if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    username = u
                    password = p
                    loginSuccess = true
                }
            }
        }
    }

    fun onUsernameChange(new: String) { username = new }
    fun onPasswordChange(new: String) { password = new }

    fun login(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (username.isNotBlank() && password.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    repository.saveCredentials(username, password)
                }
                errorMessage = null
                loginSuccess = true
                onResult(true)
            } else {
                errorMessage = "Username and password must not be empty"
                onResult(false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCredentials()
            withContext(Dispatchers.Main) {
                loginSuccess = false
                username = ""
                password = ""
                errorMessage = null
            }
        }
    }
}
