package com.example.steppet

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
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
 * Application‐Klasse, in der WorkManager‐Aufträge geplant werden.
 *
 * • PetDecayWorker: alle 30 Minuten ausführen → Hunger linear über 24 h reduzieren.
 * • DailyStepSummaryWorker: täglich um 23:00 Uhr Zusammenfassung pushen.
 *
 * Außerdem starten wir StepTrackerManager bei App‐Start und synchronisieren beim Stoppen.
 */
class StepPetApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        Log.d("StepPetApp", "Application onCreate – registering workers")
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // PetDecayWorker: alle 30 Minuten ausführen
        val decayRequest = PeriodicWorkRequestBuilder<PetDecayWorker>(
            30, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "pet_decay_work",
                ExistingPeriodicWorkPolicy.KEEP,
                decayRequest
            )

        // DailyStepSummaryWorker: täglich um 23:00 Uhr eine Zusammenfassung senden
        scheduleDailyStepSummary()
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
     * Plant DailyStepSummaryWorker so, dass er jeden Tag um 23:00 Uhr läuft.
     */
    private fun scheduleDailyStepSummary() {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val todayAt23 = now.withHour(23).withMinute(0).withSecond(0).withNano(0)
        val firstRun = if (now.isAfter(todayAt23)) {
            todayAt23.plusDays(1)
        } else {
            todayAt23
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
