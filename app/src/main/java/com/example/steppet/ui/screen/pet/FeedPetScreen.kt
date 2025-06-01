package com.example.steppet.ui.screen.pet

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.example.steppet.data.local.PetEntity
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.viewmodel.PetViewModel

@Composable
fun FeedPetScreen(
    petViewModel: PetViewModel = viewModel(
        factory = AndroidViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    // pet = aktuelles PetEntity (Default, falls null)
    val pet by petViewModel.pet.collectAsState(initial = PetEntity())
    // steps = Schrittzahl aus StepTrackerManager
    val steps by StepTrackerManager.stepsToday.collectAsState(initial = 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Schrittzähler oben rechts
        Text(
            text = "Steps: $steps",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your Pet", style = MaterialTheme.typography.headlineMedium)

            // Hunger
            Text("Hunger", style = MaterialTheme.typography.bodyLarge)
            LinearProgressIndicator(
                progress = pet.hungerLevel / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
            Text("${pet.hungerLevel}%", style = MaterialTheme.typography.bodySmall)

            // Health
            Text("Health", style = MaterialTheme.typography.bodyLarge)
            LinearProgressIndicator(
                progress = pet.health / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
            Text("${pet.health}%", style = MaterialTheme.typography.bodySmall)

            // Happiness
            Text("Happiness", style = MaterialTheme.typography.bodyLarge)
            LinearProgressIndicator(
                progress = pet.happiness / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
            Text("${pet.happiness}%", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(24.dp))

            if (pet.health == 0 || pet.happiness == 0) {
                Text(
                    "Your pet is sad and has left… 😢",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Feed-Button nur aktiv, wenn mind. 100 Schritte
                Button(
                    onClick = { petViewModel.feedPet() },
                    enabled = (steps >= 100),
                ) {
                    if (steps >= 100) {
                        Text("Feed your pet")
                    } else {
                        Text("Needs 100 steps first")
                    }
                }
            }
        }
    }
}
