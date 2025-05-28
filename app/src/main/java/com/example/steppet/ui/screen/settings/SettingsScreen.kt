package com.example.steppet.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.steppet.viewmodel.LoginViewModel

@Composable
fun SettingsScreen(
    loginVM: LoginViewModel,
    onBack: () -> Unit,
    onResetData: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var showChangeDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(loginVM.username) }

    // Dialog for “Change Username”
    if (showChangeDialog) {
        AlertDialog(
            onDismissRequest = { showChangeDialog = false },
            title   = { Text("Change Username") },
            text    = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    loginVM.changeUsername(newName)
                    showChangeDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.height(8.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedButton(onClick = { showChangeDialog = true }) {
            Text("Change Username")
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onResetData) {
            Text("Reset All Data")
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onDeleteAccount) {
            Text("Delete Account")
        }
    }
}
