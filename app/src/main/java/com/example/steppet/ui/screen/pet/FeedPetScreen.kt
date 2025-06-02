package com.example.steppet.ui.screen.pet

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.steppet.R
import com.example.steppet.data.local.PetEntity
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.viewmodel.PetViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPetScreen(
    navController: NavController,
    petViewModel: PetViewModel = viewModel(
        factory = AndroidViewModelFactory(LocalContext.current.applicationContext as Application)
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

@Composable
private fun FeedPetScreenContent(
    navController: NavController,
    petViewModel: PetViewModel,
    modifier: Modifier = Modifier
) {
    val pet by petViewModel.pet.collectAsState(initial = PetEntity())
    val steps by StepTrackerManager.stepsToday.collectAsState(initial = 0)
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("rewards", Context.MODE_PRIVATE)
    val today = LocalDate.now().toString()
    val lastRewardDate = prefs.getString("lastRewardDate", null)
    var showParticles by remember { mutableStateOf(false) }

    val showReward = steps >= 10_000 && lastRewardDate != today
    LaunchedEffect(showReward) {
        if (showReward) {
            Toast.makeText(
                context,
                "🎉 CONGRATULATIONS! You made your pet proud today!",
                Toast.LENGTH_SHORT
            ).show()
            showParticles = true
            prefs.edit().putString("lastRewardDate", today).apply()
            delay(2500)
            showParticles = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Kopfzeile: Titel + Steps‐Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Pet",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = { navController.navigate("step_history") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                ) {
                    Text("Steps: $steps", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Pet‐Bild (Basics vs. Reward)
            Crossfade(targetState = steps >= 10_000, label = "PetImageCrossfade") { reward ->
                val petImage = if (reward) R.drawable.pet_reward else R.drawable.pet_basic
                androidx.compose.foundation.Image(
                    painter = painterResource(id = petImage),
                    contentDescription = "Pet",
                    modifier = Modifier
                        .size(300.dp)
                        .padding(vertical = 16.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // Status‐Balken
            LabeledProgress("Hunger", pet.hungerLevel, MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(16.dp))
            LabeledProgress("Health", pet.health, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            LabeledProgress("Happiness", pet.happiness, MaterialTheme.colorScheme.secondary)

            Spacer(Modifier.height(128.dp))

            if (pet.health == 0 || pet.happiness == 0) {
                Text(
                    text = "Your pet is sad and has left… 😢",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Dynamischer Feed‐Button: 1 Feed pro 1 000 Schritte, +10 Hunger/Health/Happiness
                val canFeedNow = petViewModel.canFeed(steps)
                val feedsLeft = petViewModel.remainingFeeds(steps)

                Button(
                    onClick = { petViewModel.feedPet(steps) },
                    enabled = canFeedNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canFeedNow)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        contentColor = if (canFeedNow)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (canFeedNow) {
                        Text("Feed your pet ($feedsLeft left)")
                    } else {
                        val currentFeedCount = pet.feedsDoneToday
                        val todayStr = pet.lastFeedDate
                        val virtualFeedsDone = if (todayStr != LocalDate.now().toString()) 0 else currentFeedCount
                        val nextThreshold = (virtualFeedsDone + 1) * 1000
                        when {
                            steps < nextThreshold && nextThreshold <= 10_000 -> {
                                Text("Next feed unlocked at $nextThreshold steps")
                            }
                            (steps / 1000) >= 10 -> {
                                Text("All feeds done for today")
                            }
                            else -> {
                                Text("Needs $nextThreshold steps")
                            }
                        }
                    }
                }
            }
        }

        if (showParticles) {
            ConfettiOverlay()
        }
    }
}

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
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
    Text(
        text = "${(percent * 100).toInt()}%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onSettingsClick: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val displayName = user?.displayName
    val email = user?.email

    TopAppBar(
        title = {
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
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun ConfettiOverlay() {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )

    val particles = remember {
        List(30) {
            mutableStateOf(
                Offset(
                    x = (0..900).random().toFloat(),
                    y = (-200..0).random().toFloat()
                )
            )
        }
    }

    val yOffsets = remember { List(30) { Animatable(particles[it].value.y) } }

    LaunchedEffect(Unit) {
        yOffsets.forEachIndexed { index, anim ->
            launch {
                anim.animateTo(
                    targetValue = 1000f,
                    animationSpec = tween(durationMillis = 2500)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        yOffsets.forEachIndexed { index, anim ->
            Box(
                modifier = Modifier
                    .offset(
                        x = particles[index].value.x.dp,
                        y = anim.value.dp
                    )
                    .size(8.dp)
                    .background(colors[index % colors.size], shape = CircleShape)
            )
        }
    }
}
