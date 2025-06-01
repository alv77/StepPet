package com.example.steppet.ui.screen.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.repository.PetRepository
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.ui.screen.auth.LoginScreen
import com.example.steppet.ui.screen.auth.RegisterScreen
import com.example.steppet.ui.screen.pet.FeedPetScreen
import com.example.steppet.ui.screen.settings.SettingsScreen
import com.example.steppet.ui.theme.StepPetTheme
import com.example.steppet.viewmodel.LoginViewModel
import com.example.steppet.viewmodel.StepTrackerViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // PetRepository initialisieren
        val petRepo = PetRepository(this)

        setContent {
            StepPetTheme {
                // 1) ViewModels instanziieren
                val loginVM: LoginViewModel       = viewModel()
                val stepsVM: StepTrackerViewModel = viewModel()

                // 2) Initialer screenState (egal ob bereits eingeloggt oder nicht)
                var screenState by remember {
                    mutableStateOf(
                        if (auth.currentUser != null) AuthScreen.Authenticated
                        else AuthScreen.Choice
                    )
                }

                // 3) Sobald screenState == Authenticated, synchronisiere Pet + Steps
                LaunchedEffect(screenState) {
                    if (screenState == AuthScreen.Authenticated) {
                        // a) Pet aus Firestore laden und in Room schreiben
                        launch(Dispatchers.IO) {
                            petRepo.syncPetFromCloud()
                        }
                        // b) Schritte aus Firestore laden und in StateFlow schreiben
                        StepTrackerManager.loadStepsFromCloud { remoteCount ->
                            StepTrackerManager.onStepsLoaded(remoteCount)
                        }
                    }
                }

                // 4) BackHandler nur beim Login/Register
                BackHandler(
                    enabled = (screenState == AuthScreen.Login || screenState == AuthScreen.Register)
                ) {
                    screenState = AuthScreen.Choice
                }

                // 5) UI-Baum
                Box(Modifier.fillMaxSize()) {
                    when (screenState) {
                        AuthScreen.Choice -> AuthChoiceScreen(
                            onLoginSelected    = { screenState = AuthScreen.Login },
                            onRegisterSelected = { screenState = AuthScreen.Register }
                        )

                        AuthScreen.Login -> LoginScreen(
                            onLoginSuccess = { screenState = AuthScreen.Authenticated },
                            viewModel      = loginVM
                        )

                        AuthScreen.Register -> RegisterScreen(
                            onRegisterSuccess = { screenState = AuthScreen.Authenticated },
                            viewModel         = loginVM
                        )

                        AuthScreen.Authenticated -> {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            // Zeige die E-Mail des aktuellen Users
                                            Text(text = auth.currentUser?.email ?: "")
                                        },
                                        actions = {
                                            IconButton(onClick = { screenState = AuthScreen.Settings }) {
                                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                                            }
                                        }
                                    )
                                }
                            ) { innerPadding ->
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    // FeedPetScreen: zeigt Pet-Daten an und erlaubt Füttern
                                    FeedPetScreen()

                                    Spacer(Modifier.height(24.dp))

                                    // StepCountDisplay: zeigt Schritte an
                                    StepCountDisplay(viewModel = stepsVM)

                                }
                            }

                            // Logout-Button (außerhalb des Scaffold-Inhalts, unten rechts)
                            Button(
                                onClick = {
                                    loginVM.logout()
                                    screenState = AuthScreen.Choice
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .width(120.dp)
                                    .height(48.dp)
                            ) {
                                Text("Log Out")
                            }
                        }

                        AuthScreen.Settings -> {
                            SettingsScreen(
                                loginVM = loginVM,
                                onBack = { screenState = AuthScreen.Authenticated },
                                onResetData = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        AppDatabase.getInstance(this@MainActivity).clearAllTables()
                                    }
                                },
                                onDeleteAccount = {
                                    loginVM.deleteAccount { success ->
                                        if (success) {
                                            screenState = AuthScreen.Choice
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // SensorListener (Schritte zählen) starten
        StepTrackerManager.start(this)
    }

    override fun onStop() {
        super.onStop()
        // SensorListener beenden
        StepTrackerManager.stop()
    }
}

private enum class AuthScreen {
    Choice, Login, Register, Authenticated, Settings
}

/**
 * Einfacher Composable‐Helper für die Wahl zwischen Login und Register.
 */
@Composable
fun AuthChoiceScreen(
    onLoginSelected:    () -> Unit,
    onRegisterSelected: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement  = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to StepPet!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLoginSelected, Modifier.fillMaxWidth()) {
            Text("Log In")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRegisterSelected, Modifier.fillMaxWidth()) {
            Text("Register")
        }
    }
}












