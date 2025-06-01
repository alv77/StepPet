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
import com.example.steppet.data.cloud.CloudRepository
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

        // PetRepository für Cloud-Sync
        val petRepo = PetRepository(this)

        setContent {
            StepPetTheme {
                val loginVM: LoginViewModel = viewModel()
                val stepsVM: StepTrackerViewModel = viewModel()

                var screenState by remember {
                    mutableStateOf(
                        if (auth.currentUser != null) AuthScreen.Authenticated
                        else AuthScreen.Choice
                    )
                }

                LaunchedEffect(screenState) {
                    if (screenState == AuthScreen.Authenticated) {
                        launch(Dispatchers.IO) {
                            petRepo.syncPetFromCloud()
                        }
                        StepTrackerManager.loadStepsFromCloud { remoteCount ->
                            StepTrackerManager.onStepsLoaded(remoteCount)
                        }
                    }
                }

                BackHandler(
                    enabled = (screenState == AuthScreen.Login || screenState == AuthScreen.Register)
                ) {
                    screenState = AuthScreen.Choice
                }

                Box(Modifier.fillMaxSize()) {
                    when (screenState) {
                        AuthScreen.Choice -> AuthChoiceScreen(
                            onLoginSelected = { screenState = AuthScreen.Login },
                            onRegisterSelected = { screenState = AuthScreen.Register }
                        )

                        AuthScreen.Login -> LoginScreen(
                            onLoginSuccess = { screenState = AuthScreen.Authenticated },
                            viewModel = loginVM
                        )

                        AuthScreen.Register -> RegisterScreen(
                            onRegisterSuccess = { screenState = AuthScreen.Authenticated },
                            viewModel = loginVM
                        )

                        AuthScreen.Authenticated -> {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text = auth.currentUser?.displayName
                                                    ?: auth.currentUser?.email
                                                    ?: ""
                                            )
                                        },
                                        actions = {
                                            IconButton(onClick = { screenState = AuthScreen.Settings }) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Settings"
                                                )
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
                                    FeedPetScreen()
                                    Spacer(Modifier.height(24.dp))
                                    StepCountDisplay(viewModel = stepsVM)
                                }
                            }
                        }

                        AuthScreen.Settings -> {
                            SettingsScreen(
                                loginVM = loginVM,
                                onBack = { screenState = AuthScreen.Authenticated },
                                onResetData = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        AppDatabase.getInstance(this@MainActivity).clearAllTables()
                                        try {
                                            CloudRepository.deletePetInCloud()
                                        } catch (_: Exception) {}
                                        try {
                                            CloudRepository.deleteAllStepsInCloud()
                                        } catch (_: Exception) {}
                                    }
                                },
                                onLogout = {
                                    screenState = AuthScreen.Choice
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
        StepTrackerManager.start(this)
    }

    override fun onStop() {
        super.onStop()
        StepTrackerManager.stop()
    }
}

private enum class AuthScreen {
    Choice, Login, Register, Authenticated, Settings
}

@Composable
fun AuthChoiceScreen(
    onLoginSelected: () -> Unit,
    onRegisterSelected: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
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
