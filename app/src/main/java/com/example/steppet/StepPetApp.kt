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

class StepPetApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        Log.d("StepDebug", "✅ StepTrackerManager started")

        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // PetDecayWorker: alle 15 Minuten Hunger/Health/Happiness reduzieren
        val decayRequest = PeriodicWorkRequestBuilder<PetDecayWorker>(
            15, TimeUnit.MINUTES
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
        StepTrackerManager.start(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        StepTrackerManager.syncStepsToCloud() // Push steps on backgrounding
        StepTrackerManager.stop()
    }

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
