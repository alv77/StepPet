package com.example.steppet.ui.screen.pet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppet.viewmodel.PetViewModel

@Composable
fun FeedPetScreen(
    petViewModel: PetViewModel = viewModel()
) {
    // Hunger state (0–100)
    val hunger by remember { derivedStateOf { petViewModel.hunger } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pet Hunger", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Show a Material3 LinearProgressIndicator
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
