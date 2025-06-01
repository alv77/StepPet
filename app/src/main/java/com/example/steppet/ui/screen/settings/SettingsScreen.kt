package com.example.steppet.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.steppet.viewmodel.LoginViewModel

/**
 * In den Settings kann man:
 *   • Zurück‐Button (onBack)
 *   • Alle Daten zurücksetzen (onResetData)
 *   • Nutzernamen ändern (changeUsername)
 *   • Logout (onLogout)
 */
@Composable
fun SettingsScreen(
    loginVM: LoginViewModel,
    onBack: () -> Unit,
    onResetData: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // State‐Variablen für Username‐Änderung
    var newUsername by remember { mutableStateOf("") }
    var isChanging by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // 1) Back‐Button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 2) Reset All Data
        Button(
            onClick = {
                onResetData()
                Toast
                    .makeText(context, "Local and cloud data reset.", Toast.LENGTH_SHORT)
                    .show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset all data")
        }
        Spacer(modifier = Modifier.height(24.dp))

        // 3) Change Username
        Text(text = "Change Username", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newUsername,
            onValueChange = { newUsername = it },
            label = { Text("New Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (errorMsg != null) {
            Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (successMsg != null) {
            Text(text = successMsg!!, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (newUsername.isBlank()) {
                    errorMsg = "Username cannot be empty"
                    successMsg = null
                    return@Button
                }
                errorMsg = null
                successMsg = null
                isChanging = true
                loginVM.changeUsername(newUsername) { success, err ->
                    isChanging = false
                    if (success) {
                        successMsg = "Username updated"
                        Toast
                            .makeText(context, "Username successfully changed.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        errorMsg = err ?: "Failed to change username"
                    }
                }
            },
            enabled = !isChanging,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isChanging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Save Username")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // 4) Logout (nur ausloggen, Account bleibt bestehen)
        Button(
            onClick = {
                loginVM.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Out")
        }
    }
}
