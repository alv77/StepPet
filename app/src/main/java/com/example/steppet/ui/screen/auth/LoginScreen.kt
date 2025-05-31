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
 * LoginScreen zeigt zwei Textfelder (E-Mail + Passwort), einen Button zum Anmelden
 * und ggf. eine Fehlermeldung. Sobald loginWithEmail(...) aufgerufen wurde und
 * der Callback "success = true" liefert, wird onLoginSuccess() getriggert.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel
) {
    // 1) UI-Zustände: email, pass, isLoading, errorMessage
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // 2) Wenn login erfolgreich, springen wir zu onLoginSuccess
    LaunchedEffect(Unit) {
        // nichts weiter zu tun – Callback passiert in Button-onClick
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Log In", style = MaterialTheme.typography.headlineMedium)

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

        Spacer(modifier = Modifier.height(16.dp))

        // Fehlermeldung, falls vorhanden
        errorMsg?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Login-Button
        Button(
            onClick = {
                errorMsg = null
                isLoading = true

                // 3) ViewModel-Aufruf: loginWithEmail(...)
                viewModel.loginWithEmail(
                    email.trim(),
                    password
                ) { success, err ->
                    isLoading = false
                    if (success) {
                        onLoginSuccess()
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
                Text("Log in")
            }
        }
    }
}






