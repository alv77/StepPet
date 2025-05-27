package com.example.steppet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.steppet.logic.StepTrackerManager

class StepTrackerViewModel(application: Application) : AndroidViewModel(application) {
    val stepsToday = StepTrackerManager.stepsToday
}

