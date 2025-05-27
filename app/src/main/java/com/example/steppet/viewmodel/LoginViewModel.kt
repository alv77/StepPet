package com.example.steppet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.example.steppet.data.repository.LoginRepository
import kotlinx.coroutines.launch

/**
 * ViewModel managing user registration, login, and logout,
 * backed by a Room users table that persists all registered accounts.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LoginRepository(application)

    /** Bound to the username text field */
    var username by mutableStateOf("")
        private set

    /** Bound to the password text field */
    var password by mutableStateOf("")
        private set

    /** Becomes true once login or registration succeeds */
    var loginSuccess by mutableStateOf(false)
        private set

    /** Holds any error message to display under the form */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Update username as the user types */
    fun onUsernameChange(new: String) {
        username = new
    }

    /** Update password as the user types */
    fun onPasswordChange(new: String) {
        password = new
    }

    /**
     * Attempt to register a new account.
     * Calls onResult(true) if successful, false otherwise.
     */
    fun register(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || password.isBlank()) {
                errorMessage = "Username & password required"
                onResult(false)
            } else {
                val success = repo.register(username, password)
                if (success) {
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

    /**
     * Attempt to log in with the given credentials.
     * Calls onResult(true) if a matching user exists, false otherwise.
     */
    fun login(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repo.login(username, password)
            if (success) {
                errorMessage = null
                loginSuccess = true
                onResult(true)
            } else {
                errorMessage = "Invalid credentials"
                onResult(false)
            }
        }
    }

    /**
     * Log out the current session. Does not delete any users,
     * so registrations remain in the database.
     */
    fun logout() {
        loginSuccess = false
        username = ""
        password = ""
        errorMessage = null
    }
}
