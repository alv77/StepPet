package com.example.steppet.viewmodel

import android.app.Application
import android.hardware.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class StepTrackerViewModel(app: Application) : AndroidViewModel(app), SensorEventListener {
    private val sensorManager = app.getSystemService(SensorManager::class.java)
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    private val threshold = 11.5f
    private val intervalMs = 350L
    private var lastTime = 0L

    init {
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(ev: SensorEvent?) {
        if (ev?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val m = sqrt(ev.values[0]*ev.values[0] + ev.values[1]*ev.values[1] + ev.values[2]*ev.values[2])
        val now = System.currentTimeMillis()
        if (m > threshold && now - lastTime > intervalMs) {
            lastTime = now
            viewModelScope.launch { _stepsToday.emit(_stepsToday.value + 1) }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, acc: Int) {}
    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }

    /** Reset today’s count to zero */
    fun resetSteps() {
        viewModelScope.launch { _stepsToday.emit(0) }
    }
}
