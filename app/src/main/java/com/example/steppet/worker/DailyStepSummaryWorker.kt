package com.example.steppet.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.steppet.data.cloud.CloudRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DailyStepSummaryWorker läuft einmal pro Tag (geplant über WorkManager).
 * Bei doWork() wird:
 * 1) Schrittzahl für heute aus Firestore geholt.
 * 2) Sofort Notification „Du bist heute X Schritte gegangen“ angezeigt.
 * 3) Baseline-Schritte vom Sensor (STEP_COUNTER) in SharedPreferences gespeichert,
 *    damit StepTrackerManager am nächsten Morgen korrekt zählt.
 */
class DailyStepSummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val CHANNEL_ID = "daily_steps_channel"
        private const val CHANNEL_NAME = "Daily Step Summary"
        private const val NOTIF_ID = 3000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            // Kein eingeloggter User → nichts tun
            return@withContext Result.success()
        }

        // 1) Heutige Schrittzahl aus Cloud holen
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val count = try {
            CloudRepository.getStepsFromCloud(user.uid, today) ?: 0
        } catch (_: Exception) {
            0
        }

        // 2) Notification anzeigen
        showStepSummaryNotification(count)
        Log.d("DailyStepSummary", "Notification gesendet: Schritte heute = $count")

        // 3) Sensor-Baseline für STEP_COUNTER setzen
        val sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounter != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    sensorManager.unregisterListener(this)
                    val totalRaw = event?.values?.get(0)?.toInt() ?: return

                    val prefs = applicationContext.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putInt("baseline_steps", totalRaw)
                        .putString("last_step_date", today)
                        .apply()
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(
                listener,
                stepCounter,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        Result.success()
    }

    private fun showStepSummaryNotification(stepCount: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // NotificationChannel anlegen (einmalig, hier idempotent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tägliche Zusammenfassung der Schritte"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Schritte heute"
        val text = "Du bist heute $stepCount Schritte gegangen."

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID, notification)
    }
}