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

    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null
    private var appContext: Context? = null

    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    private val stepThreshold = 11.5f
    private val stepIntervalMs = 350L
    private var lastStepTime = 0L

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

    fun start(context: Context) {
        if (sensorManager == null) {
            appContext = context.applicationContext
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        // Fetch Android step count and handle baseline logic
        loadAndroidStepCount()
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelSensor = null
        appContext = null
    }

    private fun loadAndroidStepCount() {
        val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val debugMode = false

        if (stepCounterSensor == null || debugMode) {
            // Sensor not available or we're in debug mode — set baseline directly
            totalAndroidStepsRaw = 1000
            if (loadBaseline() < 0) {
                saveBaseline(1000)
                Log.d("StepDebug", "FORCED fallback baseline because sensor is missing or emulator")
            }
            syncAndroidStepsToCloud()
            return
        }

        // Real device path — wait for sensor callback
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


    private fun syncAndroidStepsToCloud() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val date = todayString()
        val baseline = loadBaseline()
        val delta = (totalAndroidStepsRaw - baseline).coerceAtLeast(0)

        CoroutineScope(Dispatchers.IO).launch {
            val firebaseCount = CloudRepository.getStepsFromCloud(uid, date) ?: 0

            val finalStepCount = maxOf(firebaseCount, delta)
            _stepsToday.value = finalStepCount

            // Save both step sources
            CloudRepository.saveStepsToCloud(
                uid = uid,
                dateString = date,
                count = finalStepCount, // accelerometer steps or delta
                androidTotal = totalAndroidStepsRaw // absolute counter
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        val now = System.currentTimeMillis()

        if (magnitude > stepThreshold && now - lastStepTime > stepIntervalMs) {
            lastStepTime = now
            val newCount = _stepsToday.value + 1
            _stepsToday.value = newCount

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

    fun onStepsLoaded(remoteCount: Int) {
        _stepsToday.value = maxOf(_stepsToday.value, remoteCount)
    }


    fun syncStepsToCloud() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = todayString()
        val count = _stepsToday.value

        CoroutineScope(Dispatchers.IO).launch {
            CloudRepository.saveStepsToCloud(uid, date, count, totalAndroidStepsRaw)
        }
    }
}
