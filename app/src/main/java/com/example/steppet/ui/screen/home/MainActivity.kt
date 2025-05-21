package com.example.steppet.ui.screen.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppet.ui.screen.auth.LoginScreen
import com.example.steppet.ui.theme.StepPetTheme
import com.example.steppet.viewmodel.LoginViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppet.ui.screen.pet.FeedPetScreen
import com.example.steppet.viewmodel.PetViewModel
import com.example.steppet.viewmodel.StepTrackerViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge rendering
        enableEdgeToEdge()

        setContent {
            StepPetTheme {
                // Obtain our LoginViewModel that holds loginSuccess & username
                val loginViewModel: LoginViewModel = viewModel()

                // Read loginSuccess as a Compose state
                val isLoggedIn by remember { derivedStateOf { loginViewModel.loginSuccess } }

                if (!isLoggedIn) {
                    // Show Login UI until loginSuccess == true
                    LoginScreen(
                        onLoginSuccess = { /* no-op: LoginScreen sets loginSuccess internally */ }
                    )
                } else {
                    setContent {
                        StepPetTheme {
                            // Obtain our LoginViewModel that holds loginSuccess & username
                            val loginViewModel: LoginViewModel = viewModel()
                            val isLoggedIn by remember { derivedStateOf { loginViewModel.loginSuccess } }

                            if (!isLoggedIn) {
                                // Show Login UI until loginSuccess == true
                                LoginScreen(onLoginSuccess = { /* no-op */ })
                            } else {
                                // Once logged in, show the FeedPetScreen
                                val petViewModel: PetViewModel = viewModel()
                                //FeedPetScreen(petViewModel)
                                val stepTrackerViewModel: StepTrackerViewModel = viewModel()
                                StepCountDisplay(stepTrackerViewModel)

                            }
                        }
                    }

                }
                }
            }
        }
    }

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    StepPetTheme {
        // In preview, pretend we're logged out to show the LoginScreen
        var fakeLoggedIn by remember { mutableStateOf(false) }
        if (!fakeLoggedIn) {
            LoginScreen(onLoginSuccess = { fakeLoggedIn = true })
        } else {
            Text("Hello Preview!", Modifier.fillMaxSize())
        }
    }
}
