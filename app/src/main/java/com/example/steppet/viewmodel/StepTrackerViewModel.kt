package com.example.steppet.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class StepTrackerViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val isSimulationMode = stepSensor == null
    private var simulatedSteps = 100

    private val prefs = application.getSharedPreferences("stepPrefs", Context.MODE_PRIVATE)

    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    private var stepsAtMidnight = prefs.getFloat("stepsAtMidnight", -1f)
    private var lastSavedDate = prefs.getString("lastDate", null)

    init {
        if (isSimulationMode) {
            viewModelScope.launch {
                while (true) {
                    simulatedSteps += 17 // simulate steps
                    _stepsToday.emit(simulatedSteps)
                    delay(3000) // every 3 seconds
                }
            }
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        val sensorValue = event?.values?.get(0) ?: return
        val today = LocalDate.now().toString()

        initializeBaselineIfNeeded(sensorValue)

        if (today != lastSavedDate) {
            stepsAtMidnight = sensorValue
            prefs.edit()
                .putString("lastDate", today)
                .putFloat("stepsAtMidnight", stepsAtMidnight)
                .apply()
            lastSavedDate = today
        }

        val currentSteps = (sensorValue - stepsAtMidnight).toInt()
        viewModelScope.launch {
            _stepsToday.emit(currentSteps.coerceAtLeast(0))
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }

    private fun initializeBaselineIfNeeded(sensorValue: Float) {
        val today = LocalDate.now().toString()
        val storedDate = prefs.getString("lastDate", null)
        val storedBaseline = prefs.getFloat("stepsAtMidnight", -1f)

        if (storedDate == null || storedBaseline < 0) {
            prefs.edit()
                .putString("lastDate", today)
                .putFloat("stepsAtMidnight", sensorValue)
                .apply()
            stepsAtMidnight = sensorValue
            lastSavedDate = today
        }
    }

}
