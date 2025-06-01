package com.example.steppet.logic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.RequiresApi
import com.example.steppet.data.cloud.CloudRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

/**
 * StepTrackerManager kümmert sich um:
 * 1) Sensor‐Logik (Beschleunigungssensor, einfache Step‐Erkennung)
 * 2) Lokales MutableStateFlow (_stepsToday) für die UI
 * 3) Cloud‐Synchronisation:
 *    • saveStepsToCloud(...) bei jedem neuen Schritt
 *    • loadStepsFromCloud(...) direkt nach Login
 */
@RequiresApi(26)
object StepTrackerManager : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null

    // Internes MutableStateFlow für die heute gezählten Schritte
    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    private val stepThreshold = 11.5f       // empirisch gewählter Threshold
    private val stepIntervalMs = 350L       // minimale Millisekunden zwischen zwei Schritten
    private var lastStepTime = 0L

    // Hilfsfunktion, um das heutige Datum als String YY-MM-DD zu bekommen
    private fun todayString(): String {
        val today = LocalDate.now()
        return today.format(DateTimeFormatter.ISO_DATE)
    }

    /**
     * Startet den Accelerometer‐Listener (aufgerufen in MainActivity.onStart()).
     */
    fun start(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelSensor?.let { sensor ->
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    /**
     * Stoppt den Accelerometer‐Listener (aufgerufen in MainActivity.onStop()).
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

        // Wenn magnitude über Threshold UND genug Zeit seit letztem Schritt
        if (magnitude > stepThreshold && now - lastStepTime > stepIntervalMs) {
            lastStepTime = now
            val newCount = _stepsToday.value + 1
            _stepsToday.value = newCount

            // → Schrittstand direkt in Firestore speichern (asynchron):
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val date = todayString()
                CoroutineScope(Dispatchers.IO).launch {
                    // CloudRepository.saveStepsToCloud(uid, heute, neuer Wert)
                    CloudRepository.saveStepsToCloud(uid, date, newCount)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Wird hier nicht benötigt
    }

    /**
     * 1) Liest den heutigen Schrittstand aus Firestore (users/{uid}/steps/{today})
     *    und ruft [onStepsLoaded] mit dem gelesenen Wert (oder 0) auf.
     *    Wird von MainActivity direkt nach Login aufgerufen.
     */
    fun loadStepsFromCloud(onLoaded: (Int) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // Kein eingelogger User → direkt 0 zurückgeben
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
     * 2) Sobald wir den Wert aus Firestore bekommen haben, setzen wir:
     *    • den internen Flow (_stepsToday) auf remoteCount
     *    • (Optional) Falls du lokale Room‐Caching für Steps nutzen möchtest, würdest du hier schreiben.
     */
    fun onStepsLoaded(remoteCount: Int) {
        _stepsToday.value = remoteCount
        // Momentan speichern wir Schritte nur in Firestore → kein lokaler Room‐Cache nötig.
    }
}



