package com.example.steppet.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.viewmodel.LoginViewModel
import io.grpc.Context

/**
 * SettingsScreen zeigt diverse Einstellungen an:
 * 1) Zurück-Button
 * 2) „Reset All Data“-Button
 * 3) Change Username
 * 4) Logout-Button
 *
 * @param loginVM     ViewModel, das Firebase-Auth-Funktionen (changeUsername, logout) bereitstellt
 * @param onBack      Callback, wenn der Nutzer auf „Back“ klickt
 * @param onResetData Callback, wenn der Nutzer „Reset all data“ wählt (löscht lokale und Cloud-Daten)
 * @param onLogout    Callback bei Logout (führt logout() aus und navigiert zurück zum Auth-Screen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    loginVM: LoginViewModel,
    onBack: () -> Unit,
    onResetData: () -> Unit,
    onLogout: () -> Unit
) {
    // Context für Toast-Meldungen
    val context = LocalContext.current

    val soundPrefs = context.getSharedPreferences("step_prefs", 0)
    var isSoundEnabled by remember {
        mutableStateOf(soundPrefs.getBoolean("sound_enabled", true))
    }


    // State-Variablen für das Change-Username-Formular
    var newUsername by remember { mutableStateOf("") }            // Eingabe für neuen Nutzernamen
    var isChanging by remember { mutableStateOf(false) }           // Flag, ob der Change-Request gerade läuft
    var errorMsg by remember { mutableStateOf<String?>(null) }     // Fehlermeldung, falls Username leer oder update fehlgeschlagen
    var successMsg by remember { mutableStateOf<String?>(null) }   // Erfolgsmeldung nach erfolgreichem Change

    var sensitivityLevel by remember {
        mutableStateOf(
            StepTrackerManager.SensitivityLevel.fromOrdinalSafe(
                context.getSharedPreferences("step_prefs", 0)
                .getInt("sensitivity_level", StepTrackerManager.SensitivityLevel.STANDARD.ordinal)
        ))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Überschrift für den Screen
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Step Sensitivity",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Slider(
            value = sensitivityLevel.ordinal.toFloat(),
            onValueChange = {
                val level = StepTrackerManager.SensitivityLevel.fromOrdinalSafe(it.toInt())
                sensitivityLevel = level
                StepTrackerManager.setSensitivity(level)
            },
            valueRange = 0f..2f,
            steps = 1,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = when (sensitivityLevel) {
                StepTrackerManager.SensitivityLevel.LOW -> "Low – More accurate, less sensitive"
                StepTrackerManager.SensitivityLevel.STANDARD -> "Standard – Balanced"
                StepTrackerManager.SensitivityLevel.HIGH -> "High – More steps, may overcount"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Enable Feed Sound",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(
                checked = isSoundEnabled,
                onCheckedChange = {
                    isSoundEnabled = it
                    soundPrefs.edit().putBoolean("sound_enabled", it).apply()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary, // White or visible on blue
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }



        // 1) Back-Button: Navigiert zurück zur vorherigen Ansicht
        Button(
            onClick = onBack,                                       // ruft den übergebenen Callback auf
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Back",
                style = MaterialTheme.typography.bodyLarge           // Standard-Textstyle
            )
        }

        // 2) Reset All Data: Löscht lokale Daten (Room, SharedPrefs) und Cloud-Daten
        Button(
            onClick = {
                onResetData()                                       // führt den Reset-Callback aus
                // Zeigt Toast als Bestätigung an
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

        // 3) Change Username: Formular-Label
        Text(
            text = "Change Username",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        // Eingabefeld für neuen Nutzernamen
        OutlinedTextField(
            value = newUsername,
            onValueChange = { newUsername = it },                    // aktualisiert State bei jeder Eingabe
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
        // Anzeige einer Fehlermeldung unterhalb des Textfelds, falls errorMsg nicht null ist
        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        // Erfolgsmeldung, falls der Username erfolgreich geändert wurde
        if (successMsg != null) {
            Text(
                text = successMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Button, um die neue Username-Änderung zu speichern
        Button(
            onClick = {
                // Validierung: Feld darf nicht leer sein
                if (newUsername.isBlank()) {
                    errorMsg = "Username cannot be empty"
                    successMsg = null
                    return@Button
                }
                // Reset von Fehlermeldung und Erfolgsmeldung
                errorMsg = null
                successMsg = null
                // setze Flag, dass Änderung läuft, um Spinner anzuzeigen
                isChanging = true

                // Aufruf des ViewModels, um Username bei Firebase zu ändern
                loginVM.changeUsername(newUsername) { success, err ->
                    isChanging = false                            // zurücksetzen des Flags
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
            enabled = !isChanging,                               // deaktivieren, während Änderung läuft
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Zeigt Spinner, solange die Änderung ausgeführt wird
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

        // 4) Logout: Meldet den User aus und ruft onLogout() auf (Navigation zum Login/Choice-Screen)
        Button(
            onClick = {
                loginVM.logout()                                   // führt FirebaseAuth.signOut() aus
                onLogout()                                         // navigiert zurück zum Auth-Screen
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
