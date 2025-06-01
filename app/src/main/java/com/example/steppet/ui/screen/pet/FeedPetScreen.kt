package com.example.steppet.ui.screen.pet

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.navigation.NavController
import com.example.steppet.data.local.PetEntity
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.viewmodel.PetViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(onSettingsClick: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    TopAppBar(
        title = {
            val displayName = user?.displayName
            val email = user?.email

            Text(
                text = when {
                    !displayName.isNullOrBlank() -> displayName
                    !email.isNullOrBlank() -> email
                    else -> "StepPet"
                }
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

@Composable
fun FeedPetScreen(
    navController: NavController,
    petViewModel: PetViewModel = viewModel(
        factory = AndroidViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    Scaffold(
        topBar = { TopBar { navController.navigate("settings") } }
    ) { padding ->
        FeedPetScreenContent(
            navController = navController,
            petViewModel = petViewModel,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun FeedPetScreenContent(
    navController: NavController,
    petViewModel: PetViewModel,
    modifier: Modifier = Modifier
) {
    val pet by petViewModel.pet.collectAsState(initial = PetEntity())
    val steps by StepTrackerManager.stepsToday.collectAsState(initial = 0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Top-right step counter button
        Button(
            onClick = { navController.navigate("step_history") },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Text("Steps: $steps", style = MaterialTheme.typography.labelLarge)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your Pet", style = MaterialTheme.typography.headlineMedium)

            LabeledProgress("Hunger", pet.hungerLevel)
            LabeledProgress("Health", pet.health)
            LabeledProgress("Happiness", pet.happiness)

            Spacer(Modifier.height(24.dp))

            if (pet.health == 0 || pet.happiness == 0) {
                Text(
                    "Your pet is sad and has left… 😢",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Button(
                    onClick = { petViewModel.feedPet() },
                    enabled = (steps >= 100),
                ) {
                    Text(if (steps >= 100) "Feed your pet" else "Needs 100 steps first")
                }
            }
        }
    }
}

@Composable
fun LabeledProgress(label: String, value: Int) {
    Text(label, style = MaterialTheme.typography.bodyLarge)
    LinearProgressIndicator(
        progress = { value / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
    )
    Text("$value%", style = MaterialTheme.typography.bodySmall)
}
