package com.example.steppet

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.worker.DailyStepSummaryWorker
import com.example.steppet.worker.PetDecayWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Application-Klasse, in der WorkManager-Aufträge geplant werden.
 *
 * • PetDecayWorker: alle 30 Minuten ausführen → Hunger linear über 24 h reduzieren,
 *   außerdem Notification, wenn Hunger von ≥20% auf <20% fällt.
 * • DailyStepSummaryWorker: täglich um 22:15 Uhr eine Zusammenfassung pushen.
 *
 * Zusätzlich enqueuen wir jeweils einen One-Time-WorkRequest, damit sowohl
 * die Hunger-Notification als auch die Step-Summary-Notification sofort
 * nach App-Start angezeigt werden (Demo-Zwecke).
 */
class StepPetApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        Log.d("StepPetApp", "Application onCreate – registering workers")
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // 1) PetDecayWorker: alle 30 Minuten ausführen
        val decayRequest = PeriodicWorkRequestBuilder<PetDecayWorker>(
            30, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "pet_decay_work",
                ExistingPeriodicWorkPolicy.KEEP,
                decayRequest
            )

        // 2) DailyStepSummaryWorker: täglich um 22:15 Uhr ausführen
        scheduleDailyStepSummary()

        // 3) Sofortige Einmal-Ausführung beider Worker, damit Notifications direkt kommen
        WorkManager.getInstance(this)
            .enqueue(OneTimeWorkRequestBuilder<PetDecayWorker>().build())
        WorkManager.getInstance(this)
            .enqueue(OneTimeWorkRequestBuilder<DailyStepSummaryWorker>().build())
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        StepTrackerManager.start(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        StepTrackerManager.syncStepsToCloud()
        StepTrackerManager.stop()
    }

    /**
     * Plant DailyStepSummaryWorker so, dass er jeden Tag um 22:15 Uhr läuft.
     */
    private fun scheduleDailyStepSummary() {
        val now = LocalDateTime.now(ZoneId.systemDefault())

        // Tageszeit 22:15 festlegen
        val todayAt2215 = now
            .withHour(22)
            .withMinute(15)
            .withSecond(0)
            .withNano(0)

        val firstRun = if (now.isAfter(todayAt2215)) {
            // Ist es bereits nach 22:15? → plane auf morgen 22:15
            todayAt2215.plusDays(1)
        } else {
            // Sonst heute um 22:15
            todayAt2215
        }
        val delayMillis = Duration.between(now, firstRun).toMillis()

        val summaryRequest = PeriodicWorkRequestBuilder<DailyStepSummaryWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "daily_step_summary_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                summaryRequest
            )
    }
}