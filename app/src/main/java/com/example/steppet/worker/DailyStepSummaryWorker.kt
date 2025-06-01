package com.example.steppet.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.steppet.data.cloud.CloudRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        if (user == null) return@withContext Result.success()

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val count = try {
            CloudRepository.getStepsFromCloud(user.uid, today) ?: 0
        } catch (_: Exception) {
            0
        }

        showStepSummaryNotification(count)
        Result.success()
    }

    private fun showStepSummaryNotification(stepCount: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
