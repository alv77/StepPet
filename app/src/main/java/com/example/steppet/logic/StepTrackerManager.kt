package com.example.steppet.logic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.steppet.data.cloud.CloudRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

object StepTrackerManager : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null

    // Internes MutableStateFlow für die aktuell gezählten Schritte
    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    private val stepThreshold = 11.5f
    private val stepIntervalMs = 350L
    private var lastStepTime = 0L

    /**
     * Startet den Accelerometer‐Listener (wird in MainActivity.onStart() aufgerufen).
     */
    fun start(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    /**
     * Stoppt den Accelerometer‐Listener (wird in MainActivity.onStop() aufgerufen).
     */
    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelSensor = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val magnitude = sqrt(
            values[0] * values[0]
                    + values[1] * values[1]
                    + values[2] * values[2]
        )
        val now = System.currentTimeMillis()
        // Wenn Sprung über Threshold UND genug Zeit seit letztem Step
        if (magnitude > stepThreshold && now - lastStepTime > stepIntervalMs) {
            lastStepTime = now
            val newCount = _stepsToday.value + 1
            _stepsToday.value = newCount

            // Speichere neuen Stand direkt in Firestore (asynchron)
            CoroutineScope(Dispatchers.IO).launch {
                CloudRepository.saveStepsToCloud(newCount)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ist hier nicht relevant
    }

    /**
     * 1) Liest die Schrittzahl aus Firestore und ruft [onStepsLoaded] auf.
     *    Wird von MainActivity direkt nach Login gestartet.
     */
    fun loadStepsFromCloud(onLoaded: (Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val remoteCount = CloudRepository.getStepsFromCloud() ?: 0
            onLoaded(remoteCount)
        }
    }

    /**
     * 2) Wird aufgerufen, sobald wir die Zahl aus Firestore haben.
     *    Setzt das interne Flow und speichert es erneut in Room (falls gewünscht).
     */
    fun onStepsLoaded(remoteCount: Int) {
        _stepsToday.value = remoteCount
        // Falls du einen lokalen Room-Cache für Steps hast,
        // könntest du ihn hier noch updaten. Aktuell speichern wir nur in Firestore.
    }
}


