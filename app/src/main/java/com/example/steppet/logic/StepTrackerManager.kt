package com.example.steppet.logic

import android.content.Context
import android.hardware.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

object StepTrackerManager : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null

    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    private val stepThreshold = 11.5f
    private val stepIntervalMs = 350L
    private var lastStepTime = 0L

    fun start(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelSensor = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        val now = System.currentTimeMillis()
        if (magnitude > stepThreshold && now - lastStepTime > stepIntervalMs) {
            lastStepTime = now
            _stepsToday.value += 1
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
