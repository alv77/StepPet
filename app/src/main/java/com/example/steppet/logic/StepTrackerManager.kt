package com.example.steppet.logic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.RequiresApi
import com.example.steppet.data.cloud.CloudRepository
import com.example.steppet.data.repository.PetRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

@RequiresApi(26)
object StepTrackerManager : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null

    // Application‐Context speichern, damit wir später PetRepository nutzen können
    private var appContext: Context? = null

    // Internes MutableStateFlow für die heute gezählten Schritte
    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    private val stepThreshold = 11.5f
    private val stepIntervalMs = 350L
    private var lastStepTime = 0L

    // Hilfsfunktion: aktuelles Datum als ISO‐String
    private fun todayString(): String {
        val today = LocalDate.now()
        return today.format(DateTimeFormatter.ISO_DATE)
    }

    /**
     * Startet den Accelerometer‐Listener (MainActivity.onStart()).
     * Speichert außerdem den Context, um PetRepository anzulegen.
     */
    fun start(context: Context) {
        if (sensorManager == null) {
            appContext = context.applicationContext

            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelSensor?.let { sensor ->
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    /**
     * Stoppt den Accelerometer‐Listener (MainActivity.onStop()).
     */
    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelSensor = null
        appContext = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val magnitude = sqrt(
            values[0] * values[0] +
                    values[1] * values[1] +
                    values[2] * values[2]
        )
        val now = System.currentTimeMillis()

        // Wenn magnitude > Threshold UND genug Zeit seit letztem Schritt
        if (magnitude > stepThreshold && now - lastStepTime > stepIntervalMs) {
            lastStepTime = now
            val newCount = _stepsToday.value + 1
            _stepsToday.value = newCount

            // → Schrittstand direkt in Firestore speichern
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val date = todayString()
                CoroutineScope(Dispatchers.IO).launch {
                    // 1) Schritte in Cloud speichern
                    CloudRepository.saveStepsToCloud(uid, date, newCount)

                    // 2) Bei jedem 100. Schritt: Health + Happiness erhöhen
                    if (newCount % 100 == 0) {
                        // PetRepository anlegen
                        appContext?.let { ctx ->
                            val petRepo = PetRepository(ctx)
                            // Health +1
                            petRepo.changeHealth(+1)
                            // Happiness +1
                            petRepo.changeHappiness(+1)
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nicht benötigt
    }

    /**
     * Liest aus Firestore den heutigen Schrittstand und übergibt ihn via Callback.
     */
    fun loadStepsFromCloud(onLoaded: (Int) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onLoaded(0)
            return
        }
        val date = todayString()
        CoroutineScope(Dispatchers.IO).launch {
            val remoteCount: Int? = CloudRepository.getStepsFromCloud(uid, date)
            onLoaded(remoteCount ?: 0)
        }
    }

    /**
     * Callback, wenn Schritte geladen sind → setzt das interne Flow.
     */
    fun onStepsLoaded(remoteCount: Int) {
        _stepsToday.value = remoteCount
    }
}
