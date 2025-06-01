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
import androidx.navigation.compose.rememberNavController
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.cloud.CloudRepository
import com.example.steppet.data.repository.PetRepository
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.ui.navigation.AppNavGraph
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
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    if (loginVM.isLoggedIn()) {
                        launch(Dispatchers.IO) {
                            PetRepository(this@MainActivity).syncPetFromCloud()
                        }
                        StepTrackerManager.loadStepsFromCloud {
                            StepTrackerManager.onStepsLoaded(it)
                        }
                    }
                }

                AppNavGraph(
                    navController = navController,
                    loginVM = loginVM,
                    stepsVM = stepsVM
                )
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
