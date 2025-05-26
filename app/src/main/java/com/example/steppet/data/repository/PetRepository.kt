package com.example.steppet.data.repository

import android.content.Context
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.local.PetEntity

class PetRepository(context: Context) {
    private val petDao = AppDatabase.getInstance(context).petDao()

    companion object {
        private const val DEFAULT_HUNGER = 100
    }

    /** Returns saved hunger or default if none. */
    suspend fun getHunger(): Int =
        petDao.getPet()?.hungerLevel ?: DEFAULT_HUNGER

    /** Inserts or updates the single PetEntity row. */
    suspend fun setHunger(level: Int) {
        val clamped = level.coerceIn(0, 100)
        petDao.upsert(PetEntity(hungerLevel = clamped))
    }
}
