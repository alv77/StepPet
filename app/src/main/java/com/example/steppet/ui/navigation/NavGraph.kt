package com.example.steppet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.steppet.ui.screen.auth.*
import com.example.steppet.ui.screen.pet.FeedPetScreen
import com.example.steppet.ui.screen.history.StepHistoryScreen
import com.example.steppet.ui.screen.settings.SettingsScreen
import com.example.steppet.viewmodel.LoginViewModel
import com.example.steppet.viewmodel.StepTrackerViewModel

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
    NavHost(navController = navController, startDestination = if (loginVM.isLoggedIn()) Screen.Home.route else Screen.Choice.route) {

        composable(Screen.Choice.route) {
            AuthChoiceScreen(
                onLoginSelected = { navController.navigate(Screen.Login.route) },
                onRegisterSelected = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginVM,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Choice.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = loginVM,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Choice.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            FeedPetScreen(navController = navController)
        }

        composable(Screen.StepHistory.route) {
            StepHistoryScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                loginVM = loginVM,
                onBack = { navController.navigate(Screen.Home.route) },
                onResetData = { /* logic moved to MainActivity */ },
                onLogout = {
                    loginVM.logout()
                    navController.navigate(Screen.Choice.route) {
                        popUpTo(0) // clear backstack
                    }
                }
            )
        }
    }
}
