package com.example.steppet.ui.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.steppet.data.cloud.CloudRepository
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.ui.screen.auth.AuthChoiceScreen
import com.example.steppet.ui.screen.auth.LoginScreen
import com.example.steppet.ui.screen.auth.RegisterScreen
import com.example.steppet.ui.screen.history.StepHistoryScreen
import com.example.steppet.ui.screen.pet.FeedPetScreen
import com.example.steppet.ui.screen.settings.SettingsScreen
import com.example.steppet.viewmodel.LoginViewModel
import com.example.steppet.viewmodel.StepTrackerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Navigation Graph für die App, jetzt mit vollständigem Reset
 * der lokalen Daten (Room, SharedPreferences, StepTracker)
 * beim erfolgreichen Login oder bei Registrierung.
 */
sealed class Screen(val route: String) {
    object Choice : Screen("auth_choice")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object StepHistory : Screen("step_history")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    loginVM: LoginViewModel,
    stepsVM: StepTrackerViewModel
) {
    NavHost(
        navController = navController,
        startDestination = if (loginVM.isLoggedIn()) Screen.Home.route else Screen.Choice.route
    ) {
        // 1. Auswahl-Screen (Login oder Register)
        composable(Screen.Choice.route) {
            AuthChoiceScreen(
                onLoginSelected = { navController.navigate(Screen.Login.route) },
                onRegisterSelected = { navController.navigate(Screen.Register.route) }
            )
        }

        // 2. Login-Screen
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginVM,
                onLoginSuccess = {
                    // Daten zurücksetzen, bevor zur Home-Screen navigiert wird
                    val context = navController.context

                    // a) Room-Datenbank komplett löschen
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.getInstance(context).clearAllTables()
                    }
                    // b) StepTrackerManager auf 0 Schritte setzen
                    StepTrackerManager.resetSteps()
                    // c) SharedPreferences löschen: Decay-, Step- und Rewards-Prefs
                    context.getSharedPreferences("decay_prefs", Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    context.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    context.getSharedPreferences("rewards", Context.MODE_PRIVATE)
                        .edit().clear().apply()

                    // d) Erst jetzt zu Home navigieren
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Choice.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Register-Screen
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = loginVM,
                onRegisterSuccess = {
                    // Daten zurücksetzen, bevor zur Home-Screen navigiert wird
                    val context = navController.context

                    // a) Room-Datenbank komplett löschen
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.getInstance(context).clearAllTables()
                    }
                    // b) StepTrackerManager auf 0 Schritte setzen
                    StepTrackerManager.resetSteps()
                    // c) SharedPreferences löschen: Decay-, Step- und Rewards-Prefs
                    context.getSharedPreferences("decay_prefs", Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    context.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    context.getSharedPreferences("rewards", Context.MODE_PRIVATE)
                        .edit().clear().apply()

                    // d) Erst jetzt zu Home navigieren
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Choice.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. Home-Screen (FeedPet)
        composable(Screen.Home.route) {
            FeedPetScreen(navController = navController)
        }

        // 5. Step-History-Screen
        composable(Screen.StepHistory.route) {
            StepHistoryScreen()
        }

        // 6. Settings-Screen
        composable(Screen.Settings.route) {
            var triggerReset by remember { mutableStateOf(false) }

            // Wenn triggerReset=true, lösche lokale Daten und Cloud-Daten
            if (triggerReset) {
                LaunchedEffect(Unit) {
                    val context = navController.context
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.getInstance(context).clearAllTables()
                        try { CloudRepository.deletePetInCloud() } catch (_: Exception) {}
                        try { CloudRepository.deleteAllStepsInCloud() } catch (_: Exception) {}
                    }
                    StepTrackerManager.resetSteps()
                    triggerReset = false
                }
            }

            SettingsScreen(
                loginVM = loginVM,
                onBack = { navController.navigate(Screen.Home.route) },
                onResetData = { triggerReset = true },
                onLogout = {
                    loginVM.logout()
                    navController.navigate(Screen.Choice.route) {
                        popUpTo(0) // Clear Backstack
                    }
                }
            )
        }
    }
}
