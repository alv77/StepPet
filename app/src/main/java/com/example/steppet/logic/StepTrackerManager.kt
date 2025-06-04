package com.example.steppet.logic

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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

    // --- Sensor references and app context ---
    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null
    private var appContext: Context? = null

    // --- Step count StateFlow exposed to observers ---
    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    // --- Accelerometer sensitivity logic ---
    private var stepThreshold = 11.5f
    private var stepIntervalMs = 350L
    private var lastStepTime = 0L

    // --- Enum for sensitivity presets with fallback logic ---
    enum class SensitivityLevel(val threshold: Float, val intervalMs: Long) {
        LOW(13.0f, 600L),
        STANDARD(11.5f, 350L),
        HIGH(10.0f, 300L);

        companion object {
            fun fromOrdinalSafe(ordinal: Int): SensitivityLevel {
                return values().getOrElse(ordinal) { STANDARD }
            }
        }
    }

    // --- SharedPreferences keys ---
    private const val PREF_SENSITIVITY = "sensitivity_level"

    // --- Save user-defined sensitivity level ---
    private fun saveSensitivity(level: SensitivityLevel) {
        getPrefs().edit().putInt(PREF_SENSITIVITY, level.ordinal).apply()
    }

    // --- Load sensitivity level from preferences ---
    private fun loadSensitivity(): SensitivityLevel {
        val ordinal = getPrefs().getInt(PREF_SENSITIVITY, SensitivityLevel.STANDARD.ordinal)
        return SensitivityLevel.fromOrdinalSafe(ordinal)
    }

    // --- Update runtime settings based on user preference ---
    fun setSensitivity(level: SensitivityLevel) {
        stepThreshold = level.threshold
        stepIntervalMs = level.intervalMs
        saveSensitivity(level)
    }

    // --- Android step counter baseline management ---
    private var totalAndroidStepsRaw = 0
    private const val PREFS_NAME = "step_prefs"
    private const val PREF_BASELINE = "baseline_steps"
    private const val PREF_LAST_DAY = "last_step_date"

    private fun todayString(): String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

    private fun getPrefs(): SharedPreferences =
        appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadBaseline(): Int =
        getPrefs().getInt(PREF_BASELINE, -1)

    private fun saveBaseline(value: Int) {
        getPrefs().edit().putInt(PREF_BASELINE, value).putString(PREF_LAST_DAY, todayString()).apply()
    }

    // --- Start accelerometer tracking and load initial step state ---
    fun start(context: Context) {
        if (sensorManager == null) {
            appContext = context.applicationContext
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        setSensitivity(loadSensitivity())

        loadAndroidStepCount()
    }

    // --- Stop tracking and clear references to avoid leaks ---
    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelSensor = null
        appContext = null
    }

    // --- Load the TYPE_STEP_COUNTER value for delta-based tracking ---
    private fun loadAndroidStepCount() {
        val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val debugMode = true

        if (stepCounterSensor == null || debugMode) {
            // Sensor not available or we're in debug mode — reset preferences
            getPrefs().edit()
                .remove(PREF_BASELINE)
                .apply()

            totalAndroidStepsRaw = 3000

            if (loadBaseline() < 0) {
                saveBaseline(1000)
                Log.d("StepDebug", "FORCED fallback baseline because sensor is missing or emulator")
            }

            syncAndroidStepsToCloud()
            return
        }

        // Register a one-time listener to get the current raw step count
        sensorManager?.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                sensorManager?.unregisterListener(this)
                totalAndroidStepsRaw = event?.values?.get(0)?.toInt() ?: 0

                if (loadBaseline() < 0) {
                    saveBaseline(totalAndroidStepsRaw)
                    Log.d("StepDebug", "Baseline set from TYPE_STEP_COUNTER: $totalAndroidStepsRaw")
                }

                syncAndroidStepsToCloud()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    // --- Hard reset of the step count (manual override) ---
    fun resetSteps() {
        _stepsToday.value = 0
    }

    // --- Calculate delta from baseline and sync it to Firestore ---
    private fun syncAndroidStepsToCloud() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val date = todayString()
        val baseline = loadBaseline()
        val delta = (totalAndroidStepsRaw - baseline).coerceAtLeast(0)

        Log.d("StepDebug", "TotalRaw = $totalAndroidStepsRaw")
        Log.d("StepDebug", "Baseline = $baseline")
        Log.d("StepDebug", "Delta = $delta")

        CoroutineScope(Dispatchers.IO).launch {
            val firebaseCount = CloudRepository.getStepsFromCloud(uid, date) ?: 0

            val finalStepCount = maxOf(firebaseCount, delta)
            _stepsToday.value = finalStepCount

            // Save both computed and raw values
            CloudRepository.saveStepsToCloud(
                uid = uid,
                dateString = date,
                count = finalStepCount, // accelerometer steps or delta
                androidTotal = totalAndroidStepsRaw // absolute counter
            )
        }
    }

    // --- Accelerometer-based fallback step detection ---
    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        val now = System.currentTimeMillis()

        if (magnitude > stepThreshold && now - lastStepTime > stepIntervalMs) {
            lastStepTime = now
            val newCount = _stepsToday.value + 1
            _stepsToday.value = newCount

            // Pet stats reward every 100 steps
            if (newCount % 100 == 0) {
                appContext?.let { ctx ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val petRepo = PetRepository(ctx)
                        petRepo.changeHealth(+1)
                        petRepo.changeHappiness(+1)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- Fetch remote step count (for merge or display) ---
    fun loadStepsFromCloud(onLoaded: (Int) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onLoaded(0)
            return
        }

        val date = todayString()
        CoroutineScope(Dispatchers.IO).launch {
            val remoteCount = CloudRepository.getStepsFromCloud(uid, date)
            onLoaded(remoteCount ?: 0)
        }
    }

    // --- Update internal count based on cloud data if higher ---
    fun onStepsLoaded(remoteCount: Int) {
        _stepsToday.value = maxOf(_stepsToday.value, remoteCount)
    }

    // --- Force-push current step count to Firestore (manual sync) ---
    fun syncStepsToCloud() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = todayString()
        val count = _stepsToday.value

        CoroutineScope(Dispatchers.IO).launch {
            CloudRepository.saveStepsToCloud(uid, date, count, totalAndroidStepsRaw)
        }
    }
}
