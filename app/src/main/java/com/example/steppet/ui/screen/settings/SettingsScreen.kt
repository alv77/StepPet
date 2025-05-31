package com.example.steppet.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.steppet.viewmodel.LoginViewModel

/**
 * Vereinfacht: In den Settings kann man
 *   • Zurück‐Button (onBack)
 *   • Alle Daten zurücksetzen (onResetData)
 *   • Konto löschen (onDeleteAccount)
 */
@Composable
fun SettingsScreen(
    loginVM: LoginViewModel,
    onBack: () -> Unit,
    onResetData: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // 1) Daten (Room) zurücksetzen
                onResetData()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset all local data")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // 2) Konto löschen und zurück zur Login‐Maske
                loginVM.deleteAccount { success ->
                    if (success) {
                        onDeleteAccount()
                    } else {
                        // hier könntest du Toast/Fehlermeldung zeigen
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete Account")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // 3) Logout
                loginVM.logout()
                onDeleteAccount() // → Navigiere zurück zur Choice/Start‐Maske
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Out")
        }
    }
}

