package com.example.steppet

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.steppet.logic.StepTrackerManager

class StepPetApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        StepTrackerManager.start(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        StepTrackerManager.stop()
    }
}
