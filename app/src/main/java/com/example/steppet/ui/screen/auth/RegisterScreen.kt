package com.example.steppet.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.steppet.viewmodel.LoginViewModel

/**
 * Einfache Registrierungs‐Composable. Verwaltet vor Ort State
 * für E-Mail, Passwort und „Passwort bestätigen“.
 * Ruft LoginViewModel.registerWithEmail(...) auf.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    viewModel: LoginViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Register", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        errorMsg?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    // 1) Prüfen, ob Passwörter übereinstimmen
                    if (password != confirmPassword) {
                        errorMsg = "Passwords do not match"
                        return@Button
                    }
                    errorMsg = null
                    isLoading = true

                    viewModel.registerWithEmail(email.trim(), password) { success, err ->
                        isLoading = false
                        if (success) {
                            onRegisterSuccess()
                        } else {
                            errorMsg = err ?: "Unknown error"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }
        }
    }
}





