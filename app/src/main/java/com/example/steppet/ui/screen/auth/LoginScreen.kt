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
 * Einfache Login‐Composable. Verwaltet vor Ort State für E-Mail/PW,
 * und ruft im Button‐Klick die Methode
 * LoginViewModel.loginWithEmail(email, password) auf.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel
) {
    // 1) Lokaler UI‐State für E-Mail, Passwort, Fehlermeldung, Lade‐Anzeige
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // E-Mail‐Feld
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Passwort‐Feld
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Fehlermeldung anzeigen, falls vorhanden
        errorMsg?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Login‐Button oder Lade‐Indikator
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    errorMsg = null
                    isLoading = true

                    viewModel.loginWithEmail(email.trim(), password) { success, err ->
                        isLoading = false
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMsg = err ?: "Unknown error"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log In")
            }
        }
    }
}





