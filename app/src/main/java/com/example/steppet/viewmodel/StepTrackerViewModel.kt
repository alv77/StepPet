package com.example.steppet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.steppet.logic.StepTrackerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StepTrackerViewModel : ViewModel() {
    val stepsToday: StateFlow<Int> = StepTrackerManager.stepsToday
}

