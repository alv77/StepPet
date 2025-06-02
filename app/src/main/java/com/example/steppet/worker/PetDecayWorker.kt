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
 * basierend auf dem Zeitstempel in SharedPreferences ("decay_prefs" → "last_decay_time").
 *
 * • Wenn Hunger > 0: Health − 1, Happiness − 1
 * • Wenn Hunger = 0: Health − 5, Happiness − 5
 *
 * Zusätzlich: Notification, falls Hunger < 20 %.
 *
 * Ablauf:
 * 1. Lese last_decay_time (Millis) aus SharedPreferences.
 * 2. Berechne elapsedMinutes seit last_decay_time.
 * 3. pointsToReduce = floor(elapsedMinutes * 100 / 1440).
 *    → Bei 1440 Min (24 h) reduziert Hunger um 100.
 * 4. Reduziere Hunger, setze neue Health/Happiness‐Werte.
 * 5. Falls Hunger < 20 % und heute noch keine Notification gesendet, sende sie.
 * 6. Aktualisiere last_decay_time = jetzt.
 */
class PetDecayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repo = PetRepository(context)
    private val prefs = context.getSharedPreferences("decay_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_LAST_DECAY = "last_decay_time"
        private const val PREF_KEY_HUNGER_NOTIF_SENT = "hunger_notif_sent_date"

        // 24 Stunden in Minuten
        private const val MINUTES_IN_24H = 24 * 60

        // Schwelle, ab der wir eine Hunger‐Notification senden
        private const val HUNGER_THRESHOLD = 20

        private const val CHANNEL_ID = "pet_hunger_channel"
        private const val CHANNEL_NAME = "Pet Hunger Alerts"
        private const val NOTIF_ID = 2000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            // 1) Lese letzten Decay‐Zeitpunkt. Wenn nicht vorhanden, initialisiere und beende.
            val lastDecay = prefs.getLong(PREF_KEY_LAST_DECAY, now)
            if (lastDecay == 0L) {
                prefs.edit().putLong(PREF_KEY_LAST_DECAY, now).apply()
                return@withContext Result.success()
            }

            // 2) Berechne vergangene Minuten
            val elapsedMillis = now - lastDecay
            val elapsedMinutes = (elapsedMillis / 60000).toInt()

            if (elapsedMinutes < 1) {
                // Weniger als 1 Minute vergangen → nichts reduzieren
                return@withContext Result.success()
            }

            // 3) Berechne, um wie viele Hunger‐Punkte gesunken werden muss:
            //    floor(elapsedMinutes * 100 / 1440)
            val pointsToReduce = floor(elapsedMinutes * 100.0 / MINUTES_IN_24H).toInt()
            if (pointsToReduce < 1) {
                // Noch kein voller Punkt zusammengekommen
                return@withContext Result.success()
            }

            // 4) Lese aktuellen Hunger, reduziere ihn
            val currentHunger = repo.getHunger()
            val newHunger = (currentHunger - pointsToReduce).coerceAtLeast(0)

            // 5) Aktualisiere Hunger
            repo.setHunger(newHunger)

            // 6) Reduziere Health/Happiness
            if (newHunger == 0) {
                repo.changeHealth(-5)
                repo.changeHappiness(-5)
            } else {
                repo.changeHealth(-1)
                repo.changeHappiness(-1)
            }

            // 7) Notification, falls Hunger < HUNGER_THRESHOLD und heute noch nicht gesendet
            val todayString = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val sentDate = prefs.getString(PREF_KEY_HUNGER_NOTIF_SENT, "")
            if (newHunger < HUNGER_THRESHOLD && sentDate != todayString) {
                showHungerNotification(newHunger)
                prefs.edit().putString(PREF_KEY_HUNGER_NOTIF_SENT, todayString).apply()
            }

            // 8) Setze last_decay_time = jetzt
            prefs.edit().putLong(PREF_KEY_LAST_DECAY, now).apply()

            Result.success()
        } catch (e: Exception) {
            Log.e("PetDecayWorker", "Fehler in doWork: ${e.localizedMessage}")
            Result.retry()
        }
    }

    private fun showHungerNotification(hunger: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
