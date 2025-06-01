// File: app/src/main/java/com/example/steppet/ui/screen/settings/SettingsScreen.kt
package com.example.steppet.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.steppet.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    loginVM: LoginViewModel,
    onBack: () -> Unit,
    onResetData: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // State-Variablen für Username-Änderung
    var newUsername by remember { mutableStateOf("") }
    var isChanging by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1) Back-Button
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Back",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // 2) Reset All Data
        Button(
            onClick = {
                onResetData()
                Toast
                    .makeText(context, "Local and cloud data reset.", Toast.LENGTH_SHORT)
                    .show()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Reset all data",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3) Change Username
        Text(
            text = "Change Username",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedTextField(
            value = newUsername,
            onValueChange = { newUsername = it },
            label = {
                Text(
                    text = "New Username",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                /* textColor entfernt, da es hier nicht existiert */
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                containerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (successMsg != null) {
            Text(
                text = successMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
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
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isChanging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Save Username",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4) Logout
        Button(
            onClick = {
                loginVM.logout()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
