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
    // PetViewModel wird hier via AndroidViewModelFactory instanziiert, um den Application-Context zu liefern
    petViewModel: PetViewModel = viewModel(
        factory = AndroidViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    // Scaffold stellt das Grundlayout mit TopBar und Content-Bereich bereit
    Scaffold(
        topBar = {
            // TopBar enthält den Titel und eine Settings-Schaltfläche, die zur Settings-Screen navigiert
            TopBar { navController.navigate("settings") }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        // FeedPetScreenContent ist der eigentliche Bildschirminhalt, mit Padding vom Scaffold berücksichtigt
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
    // Abonniere den aktuellen Pet-Zustand als State, initial mit Default-PetEntity
    val pet by petViewModel.pet.collectAsState(initial = PetEntity())
    // Abonniere die heutigen Schritte über StepTrackerManager
    val steps by StepTrackerManager.stepsToday.collectAsState(initial = 0)
    val context = LocalContext.current

    // SharedPreferences für „Rewards“ (belohntes Jubiläum, wenn 10.000 Schritte erreicht)
    val prefs = context.getSharedPreferences("rewards", Context.MODE_PRIVATE)
    val today = LocalDate.now().toString()
    val lastRewardDate = prefs.getString("lastRewardDate", null)
    // State, ob die Konfetti-Animation angezeigt werden soll
    var showParticles by remember { mutableStateOf(false) }

    // Bedingung, ob der User heute erstmals 10.000 Schritte erreicht hat
    val showReward = steps >= 10_000 && lastRewardDate != today
    LaunchedEffect(showReward) {
        if (showReward) {
            // Zeige eine Toast-Nachricht zur Belohnung
            Toast.makeText(
                context,
                "🎉 CONGRATULATIONS! You made your pet proud today!",
                Toast.LENGTH_SHORT
            ).show()
            showParticles = true
            // Speichere Datum, damit Belohnung nur einmal pro Tag angezeigt wird
            prefs.edit().putString("lastRewardDate", today).apply()
            delay(2500)              // Warte 2,5 Sekunden, während die Konfetti-Animation läuft
            showParticles = false    // Schalte Konfetti-Animation aus
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
            // Kopfzeile mit Titel „Your Pet“ und einem Button, der zur Step-History navigiert
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
                    // Anzeige der aktuellen Schritte in der Kopfzeile
                    Text("Steps: $steps", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(32.dp)) // Abstand

            // Crossfade-Animation: wenn steps >= 10.000, zeige das belohnte Pet-Bild, sonst das Basis-Bild
            Crossfade(targetState = steps >= 10_000, label = "PetImageCrossfade") { reward ->
                // Wähle Bildressource: pet_reward.jpg oder pet_basic.png
                val petImage = if (reward) R.drawable.pet_reward else R.drawable.pet_basic
                androidx.compose.foundation.Image(
                    painter = painterResource(id = petImage),
                    contentDescription = "Pet",
                    modifier = Modifier
                        .size(300.dp)
                        .padding(vertical = 16.dp)
                )
            }

            Spacer(Modifier.height(32.dp)) // Abstand

            // Zeige die drei Status-Balken für Hunger, Health und Happiness
            LabeledProgress("Hunger", pet.hungerLevel, MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(16.dp))
            LabeledProgress("Health", pet.health, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            LabeledProgress("Happiness", pet.happiness, MaterialTheme.colorScheme.secondary)

            Spacer(Modifier.height(128.dp)) // größerer Abstand vor Button

            // Wenn das Pet „tot“ ist (health == 0 oder happiness == 0), zeige traurige Nachricht
            if (pet.health == 0 || pet.happiness == 0) {
                Text(
                    text = "Your pet is sad and has left… 😢",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Ansonsten: Feed-Button aktivieren, wenn noch Feeds möglich sind
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
                        // Zeige Anzahl verbleibender Feeds an, z.B. „Feed your pet (3 left)“
                        Text("Feed your pet ($feedsLeft left)")
                    } else {
                        // Wenn keine Feeds mehr möglich sind, weise auf nächste Schwelle hin oder dass alle Feeds erledigt sind
                        val currentFeedCount = pet.feedsDoneToday
                        val todayStr = pet.lastFeedDate
                        // VirtualFeedsDone = 0, falls heute noch kein Feed an diesem Datum war
                        val virtualFeedsDone = if (todayStr != LocalDate.now().toString()) 0 else currentFeedCount
                        val nextThreshold = (virtualFeedsDone + 1) * 1000 // Nächste 1.000 Schritte-Schwelle
                        when {
                            // Wenn aktuelle Schritte noch unter nächster Schwelle und unter 10.000
                            steps < nextThreshold && nextThreshold <= 10_000 -> {
                                Text("Next feed unlocked at $nextThreshold steps")
                            }
                            // Wenn schon 10 Feeds (10.000 Schritte/Feed) erreicht sind
                            (steps / 1000) >= 10 -> {
                                Text("All feeds done for today")
                            }
                            else -> {
                                // Fallback: benötigte Schritte anzeigen
                                Text("Needs $nextThreshold steps")
                            }
                        }
                    }
                }
            }
        }

        // Wenn heute belohntes Jubiläum, zeige Konfetti-Overlay
        if (showParticles) {
            ConfettiOverlay()
        }
    }
}

/**
 * LabeledProgress zeigt einen Label-Text, einen LinearProgressIndicator und
 * den prozentualen Wert darunter.
 *
 * @param label       Beschriftung (z.B. "Hunger")
 * @param value       Wert zwischen 0 und 100
 * @param progressColor Farbe des gefüllten Teils des Balkens
 */
@Composable
private fun LabeledProgress(
    label: String,
    value: Int,
    progressColor: androidx.compose.ui.graphics.Color
) {
    // Prozent-Wert 0f..1f, abgesichert gegen Werte außerhalb [0,100]
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
    // Aktuellen User aus FirebaseAuth abrufen, um ggf. displayName oder E-Mail anzuzeigen
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
            // Icon-Button für Settings, ruft onSettingsClick() auf
            IconButton(onClick = onSettingsClick) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,     // Hintergrund des AppBar
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer, // Textfarbe des Titels
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer // Farbe des Icons
        )
    )
}

/**
 * ConfettiOverlay zeichnet kleine bunte Kreise, die von oben nach unten fallen,
 * um eine Konfetti-Animation zu simulieren.
 */
@Composable
fun ConfettiOverlay() {
    // Drei mögliche Farbtöne aus dem Theme
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )

    // Erzeuge 30 Partikel mit zufälligen Startpositionen (x, y)
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

    // Für jedes Partikel wird eine Animatable für die y-Position erstellt
    val yOffsets = remember { List(30) { Animatable(particles[it].value.y) } }

    // Starte bei Kompositionsstart alle Animationen parallel
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
        // Zeichne jedes Partikel als kleinen kreisförmigen Box
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
