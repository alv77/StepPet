package com.example.steppet.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes

object SoundUtils {

    private const val PREFS_NAME = "step_prefs"
    private const val PREF_SOUND_ENABLED = "sound_enabled"

    fun playSound(context: Context, @RawRes soundResId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        val isSoundEnabled = prefs.getBoolean(PREF_SOUND_ENABLED, true)

        if (!isSoundEnabled) {
            Log.d("SoundUtils", "Sound disabled by user settings")
            return
        }

        try {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            if (mediaPlayer == null) {
                Log.e("SoundUtils", "MediaPlayer is null – likely bad resource")
                return
            }
            mediaPlayer.setOnCompletionListener {
                it.release()
                Log.d("SoundUtils", "Sound playback finished and released")
            }
            mediaPlayer.start()
            Log.d("SoundUtils", "Started playing sound")
        } catch (e: Exception) {
            Log.e("SoundUtils", "Exception while playing sound", e)
        }
    }
}
