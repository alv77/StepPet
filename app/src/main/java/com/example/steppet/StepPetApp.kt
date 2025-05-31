package com.example.steppet

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.worker.PetDecayWorker
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class StepPetApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        // explizit die Application-Variante von onCreate() aufrufen:
        super<Application>.onCreate()

        // Test: Kann ich FirebaseAuth instanziieren?
        val auth = FirebaseAuth.getInstance()
        Log.d("FirebaseTest", "FirebaseAuth initialisiert: $auth")

        // Lifecycle‐Observer für StepTrackerManager
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // WorkManager: stündliche Decay‐Arbeit planen
        val decayRequest = PeriodicWorkRequestBuilder<PetDecayWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                /* uniqueWorkName: */ "pet_decay_work",
                /* existingWorkPolicy: */ ExistingPeriodicWorkPolicy.KEEP,
                decayRequest
            )
    }

    override fun onStart(owner: LifecycleOwner) {
        StepTrackerManager.start(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        StepTrackerManager.stop()
    }
}


