package com.example.steppet.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.steppet.data.repository.PetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.floor

/**
 * PetDecayWorker reduziert den Hunger linear über 24 Stunden (100 → 0),
 * führt alle 30 Minuten aus (geplant in StepPetApp). Zeigt JEDES MAL eine
 * Notification an, falls der Hunger gerade von ≥20% auf <20% gefallen ist.
 *
 * Ablauf:
 * 1. Lese last_decay_time (Millis) und last_hunger (Int) aus SharedPreferences.
 * 2. Lese aktuellen Hunger (aus Room über PetRepository).
 * 3. Wenn (last_hunger ≥ HUNGER_THRESHOLD && current_hunger < HUNGER_THRESHOLD), sende Notification.
 * 4. Berechne elapsedMinutes seit last_decay_time.
 * 5. Wenn elapsedMinutes ≥ 1, berechne pointsToReduce = floor(elapsedMinutes * 100 / 1440).
 *    → Reduziere Hunger um diesen Betrag, speichere neuen Hunger in Room.
 *    → Passe Health/Happiness an.
 * 6. Speichere nowMillis als new last_decay_time und speichere newHunger als last_hunger.
 */
class PetDecayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repo = PetRepository(context)
    private val prefs = context.getSharedPreferences("decay_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_LAST_DECAY = "last_decay_time"
        private const val PREF_KEY_LAST_HUNGER = "last_hunger_value"

        // 24 Stunden in Minuten
        private const val MINUTES_IN_24H = 24 * 60

        // Schwelle, ab der wir Benachrichtigung schicken
        private const val HUNGER_THRESHOLD = 20

        private const val CHANNEL_ID = "pet_hunger_channel"
        private const val CHANNEL_NAME = "Pet Hunger Alerts"
        private const val NOTIF_ID = 2000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val nowMillis = System.currentTimeMillis()

            // 1) Lese letzten Decay-Zeitpunkt (Millis) und letzten Hunger (Int) aus Prefs.
            val lastDecay = prefs.getLong(PREF_KEY_LAST_DECAY, 0L)
            val lastHunger = prefs.getInt(PREF_KEY_LAST_HUNGER, -1)

            // 2) Lese aktuellen Hunger aus Room
            val currentHunger = repo.getHunger()

            Log.d("PetDecayWorker", "doWork(): lastHunger=$lastHunger, currentHunger=$currentHunger")

            // 3) Wenn lastHunger noch nicht gesetzt (<0), initialisiere ihn, aber sende keine Notification
            if (lastHunger < 0) {
                prefs.edit()
                    .putLong(PREF_KEY_LAST_DECAY, nowMillis)
                    .putInt(PREF_KEY_LAST_HUNGER, currentHunger)
                    .apply()
                return@withContext Result.success()
            }

            // 4) Wenn Hunger gerade von ≥ HUNGER_THRESHOLD auf < HUNGER_THRESHOLD gefallen ist, sende Notification
            if (lastHunger >= HUNGER_THRESHOLD && currentHunger < HUNGER_THRESHOLD) {
                showHungerNotification(currentHunger)
            }

            // 5) Berechne vergangene Minuten seit lastDecay
            val elapsedMinutes = ((nowMillis - lastDecay) / 60000).toInt()
            if (elapsedMinutes >= 1) {
                // 6) Berechne, um wie viele Prozentpunkte Hunger abziehen
                val pointsToReduce = floor(elapsedMinutes * 100.0 / MINUTES_IN_24H).toInt()
                if (pointsToReduce >= 1) {
                    // 7) Reduziere Hunger um pointsToReduce (auf mindestens 0)
                    val newHunger = (currentHunger - pointsToReduce).coerceAtLeast(0)
                    repo.setHunger(newHunger)

                    // 8) Passe Health/Happiness an
                    if (newHunger == 0) {
                        repo.changeHealth(-5)
                        repo.changeHappiness(-5)
                    } else {
                        repo.changeHealth(-1)
                        repo.changeHappiness(-1)
                    }

                    // 9) Speichere neuen Hunger als last_hunger
                    prefs.edit().putInt(PREF_KEY_LAST_HUNGER, newHunger).apply()
                }
                // 10) Aktualisiere last_decay_time
                prefs.edit().putLong(PREF_KEY_LAST_DECAY, nowMillis).apply()
            } else {
                // Wenn weniger als 1 Minute vergangen ist, speichere currentHunger als last_hunger
                prefs.edit().putInt(PREF_KEY_LAST_HUNGER, currentHunger).apply()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("PetDecayWorker", "Fehler in doWork: ${e.localizedMessage}")
            Result.retry()
        }
    }

    /**
     * Baut und sendet eine Notification, wenn der Hunger unter 20 % fällt.
     */
    private fun showHungerNotification(hunger: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // NotificationChannel anlegen (einmalig, hier idempotent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigung, wenn dein Pet hungrig wird"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Dein Pet ist hungrig!"
        val text = "Hunger‐Level: $hunger %. Füttere dein Pet, damit es gesund bleibt."

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID, notification)
    }
}