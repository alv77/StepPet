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
 * ViewModel for handling user login/logout and secure credential storage.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LoginRepository(application)

    /** The username text field. */
    var username by mutableStateOf("")
        private set

    /** The password text field. */
    var password by mutableStateOf("")
        private set

    /** True once login has succeeded (either via manual login or auto‐login). */
    var loginSuccess by mutableStateOf(false)
        private set

    /** An error message to show below the password field. */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        // ID 3: Load any stored credentials and auto‐login if present
        viewModelScope.launch(Dispatchers.IO) {
            val storedUser = repository.getUsername()
            val storedPass = repository.getPassword()
            if (!storedUser.isNullOrBlank() && !storedPass.isNullOrBlank()) {
                // Switch to main thread to update Compose state
                withContext(Dispatchers.Main) {
                    username = storedUser
                    password = storedPass
                    loginSuccess = true
                }
            }
        }
    }

    /** Called when the user edits the username field. */
    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }

    /** Called when the user edits the password field. */
    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    /**
     * Attempts login with the current username/password.
     * On success, saves credentials securely and flips loginSuccess.
     */
    fun login(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (username.isNotBlank() && password.isNotBlank()) {
                // Persist credentials off the main thread
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

    /** Logs out the user and clears all stored credentials. */
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
