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

class PetDecayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repo = PetRepository(context)
    private val prefs = context.getSharedPreferences("pet_prefs", Context.MODE_PRIVATE)

    companion object {
        // Produktionswert: Hunger-Notification erst auslösen, wenn Hunger < 20
        private const val HUNGER_THRESHOLD = 20
        private const val PREF_KEY_HUNGER_NOTIF_SENT = "hunger_notif_sent_date"
        private const val CHANNEL_ID = "pet_hunger_channel"
        private const val CHANNEL_NAME = "Pet Hunger Alerts"
        private const val NOTIF_ID = 2000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val currentHunger = repo.getHunger()
            Log.d("PetDecayWorker", "doWork gestartet: currentHunger=$currentHunger")

            val newHunger = (currentHunger - 1).coerceAtLeast(0)
            repo.setHunger(newHunger)
            Log.d("PetDecayWorker", "Hunger reduziert: newHunger=$newHunger")

            repo.changeHealth(-1)
            repo.changeHappiness(-1)

            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val sentDate = prefs.getString(PREF_KEY_HUNGER_NOTIF_SENT, "")
            Log.d(
                "PetDecayWorker",
                "Prüfung Notification: newHunger=$newHunger, sentDate=$sentDate, heute=$today"
            )

            if (newHunger < HUNGER_THRESHOLD && sentDate != today) {
                Log.d("PetDecayWorker", "Bedingung erfüllt – sende Hunger-Notification")
                showHungerNotification(newHunger)
                prefs.edit().putString(PREF_KEY_HUNGER_NOTIF_SENT, today).apply()
            } else {
                Log.d(
                    "PetDecayWorker",
                    "Keine Notification (newHunger<$HUNGER_THRESHOLD? ${newHunger < HUNGER_THRESHOLD}, sentDate!=heute? ${sentDate != today})"
                )
            }

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
                description = "Benachrichtigung, wenn das Pet hungrig wird"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Dein Pet ist hungrig!"
        val text = "Hunger‐Level: $hunger%. Füttere dein Pet, damit es gesund bleibt."

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
