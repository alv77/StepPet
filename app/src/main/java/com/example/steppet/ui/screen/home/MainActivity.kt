package com.example.steppet.ui.screen.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppet.ui.screen.auth.LoginScreen
import com.example.steppet.ui.screen.auth.RegisterScreen
import com.example.steppet.ui.screen.pet.FeedPetScreen
import com.example.steppet.ui.theme.StepPetTheme
import com.example.steppet.viewmodel.LoginViewModel
import com.example.steppet.viewmodel.PetViewModel
import com.example.steppet.viewmodel.StepTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepPetTheme {
                val loginViewModel: LoginViewModel        = viewModel()
                val petViewModel:   PetViewModel          = viewModel()
                val stepsViewModel: StepTrackerViewModel  = viewModel()

                var screenState by remember { mutableStateOf(AuthScreen.Choice) }

                // If already logged in, go straight to Authenticated
                LaunchedEffect(loginViewModel.loginSuccess) {
                    if (loginViewModel.loginSuccess) {
                        screenState = AuthScreen.Authenticated
                    }
                }

                // Container for all states
                Box(Modifier.fillMaxSize()) {
                    // Intercept back only on Login/Register
                    BackHandler(enabled = screenState in listOf(AuthScreen.Login, AuthScreen.Register)) {
                        screenState = AuthScreen.Choice
                    }

                    when (screenState) {
                        AuthScreen.Choice -> AuthChoiceScreen(
                            onLoginSelected    = { screenState = AuthScreen.Login },
                            onRegisterSelected = { screenState = AuthScreen.Register }
                        )

                        AuthScreen.Login -> LoginScreen(
                            onLoginSuccess = { screenState = AuthScreen.Authenticated },
                            viewModel      = loginViewModel
                        )

                        AuthScreen.Register -> RegisterScreen(
                            onRegisterSuccess = { screenState = AuthScreen.Authenticated },
                            viewModel         = loginViewModel
                        )

                        AuthScreen.Authenticated -> {
                            // Use a Box so we can overlay the Log Out button
                            Box(Modifier.fillMaxSize()) {
                                // Main content with TopAppBar
                                Scaffold(
                                    topBar = {
                                        TopAppBar(
                                            title = { Text("Hello, ${loginViewModel.username}") }
                                        )
                                    }
                                ) { innerPadding ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        FeedPetScreen(petViewModel)
                                        Spacer(Modifier.height(24.dp))
                                        StepCountDisplay(stepsViewModel)
                                    }
                                }

                                // Rectangular Log Out button at bottom-end
                                Button(
                                    onClick = {
                                        loginViewModel.logout()
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
                        }
                    }
                }
            }
        }
    }
}

private enum class AuthScreen { Choice, Login, Register, Authenticated }

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
