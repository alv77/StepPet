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
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.steppet.data.local.PetEntity
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.viewmodel.PetViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopBar { navController.navigate("settings") }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        FeedPetScreenContent(
            navController = navController,
            petViewModel = petViewModel,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onSettingsClick: () -> Unit) {
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
                },
                style = MaterialTheme.typography.titleLarge

            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"

                )
            }
        },
        //
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun FeedPetScreenContent(
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
        // Top-right Schrittzähler-Button
        Button(
            onClick = { navController.navigate("step_history") },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Text(
                text = "Steps: $steps",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Pet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Hunger
            LabeledProgress(
                label = "Hunger",
                value = pet.hungerLevel,
                progressColor = MaterialTheme.colorScheme.tertiary
            )

            // Health
            LabeledProgress(
                label = "Health",
                value = pet.health,
                progressColor = MaterialTheme.colorScheme.primary
            )

            // Happiness
            LabeledProgress(
                label = "Happiness",
                value = pet.happiness,
                progressColor = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(24.dp))

            if (pet.health == 0 || pet.happiness == 0) {
                Text(
                    text = "Your pet is sad and has left… 😢",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Button(
                    onClick = { petViewModel.feedPet() },
                    enabled = (steps >= 100),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (steps >= 100)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        contentColor = if (steps >= 100)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (steps >= 100) "Feed your pet" else "Needs 100 steps first",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/**
 * Zeigt ein Label + ProgressBar + Prozentwert an.
 * - `value` läuft 0–100.
 * - `progressColor` bestimmt die Farbe der ProgressBar.
 */
@Composable
private fun LabeledProgress(
    label: String,
    value: Int,
    progressColor: androidx.compose.ui.graphics.Color
) {
    val percent = (value / 100f).coerceIn(0f, 1f)

    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    LinearProgressIndicator(
        progress = percent,
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
    )
    Text(
        text = "${(percent * 100).roundToInt()}%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground
    )
}
