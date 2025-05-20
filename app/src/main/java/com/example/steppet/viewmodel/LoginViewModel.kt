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
 * ViewModel for handling user login and logout.
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

    /** Updates the username as the user types. */
    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }

    /** Updates the password as the user types. */
    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    /**
     * Attempts login with the current username/password.
     * On success, saves credentials and invokes onResult(true).
     */
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

    /** Logs out the user and clears stored credentials. */
    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCredentials()
            loginSuccess = false
            username = ""
            password = ""
            errorMessage = null
        }
    }
}
