package com.example.steppet.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.steppet.viewmodel.LoginViewModel

/**
 * RegisterScreen zeigt drei Textfelder (E-Mail, Passwort, Passwort bestätigen) und
 * einen Button, um ein neues Konto anzulegen. Stimmt "Passwort" != "Passwort bestätigen",
 * wird automatisch eine Fehlermeldung angezeigt.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    viewModel: LoginViewModel
) {
    // 1) UI-Zustände: email, password, confirmPassword, isLoading, errorMsg
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registrieren", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // E-Mail-TextField
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "E-Mail") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Passwort-TextField
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Passwort") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Passwort bestätigen
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(text = "Passwort bestätigen") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Fehlermeldung, falls vorhanden
        errorMsg?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Register-Button
        Button(
            onClick = {
                // 2) Vor dem ViewModel-Aufruf prüfen wir, ob Passwörter übereinstimmen
                if (password != confirmPassword) {
                    errorMsg = "Passwörter stimmen nicht überein"
                    return@Button
                }

                errorMsg = null
                isLoading = true

                // 3) ViewModel-Aufruf: registerWithEmail(...)
                viewModel.registerWithEmail(
                    email.trim(),
                    password
                ) { success, err ->
                    isLoading = false
                    if (success) {
                        onRegisterSuccess()
                    } else {
                        errorMsg = err ?: "Unbekannter Fehler"
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register")
            }
        }
    }
}






