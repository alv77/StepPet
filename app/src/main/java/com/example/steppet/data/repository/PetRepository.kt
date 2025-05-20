package com.example.steppet.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Simple persistence for the pet’s hunger level (0–100).
 */
class PetRepository(context: Context) {

    private val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "pet_prefs",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_HUNGER = "hunger_level"
        private const val DEFAULT_HUNGER = 100
    }

    /** Returns the saved hunger level (0–100). */
    fun getHunger(): Int =
        prefs.getInt(KEY_HUNGER, DEFAULT_HUNGER)

    /** Saves the new hunger level (clamped 0–100). */
    fun setHunger(level: Int) {
        val clamped = level.coerceIn(0, 100)
        prefs.edit()
            .putInt(KEY_HUNGER, clamped)
            .apply()
    }
}
