package com.example.steppet.ui.screen.pet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppet.viewmodel.PetViewModel
import com.example.steppet.logic.StepTrackerManager

@Composable
fun FeedPetScreen(
    petViewModel: PetViewModel = viewModel()
) {
    val hunger by remember { derivedStateOf { petViewModel.hunger } }
    val steps by StepTrackerManager.stepsToday.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // ⏫ Step count in top-right corner
        Text(
            text = "Steps: $steps",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
        )

        // 🐶 Main content: pet hunger UI centered
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text("Pet Hunger", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = hunger / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("$hunger%", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))

            Button(onClick = { petViewModel.feedPet() }) {
                Text("Feed Pet")
            }
        }
    }
}
