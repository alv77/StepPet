package com.example.steppet.data.repository

import android.content.Context
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.local.PetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PetRepository(context: Context) {
    private val petDao = AppDatabase.getInstance(context).petDao()

    companion object {
        private const val DEFAULT_ID = 0L
        private const val DEFAULT_HUNGER = 100
        private const val DEFAULT_HEALTH = 100
        private const val DEFAULT_HAPPINESS = 100
    }

    /**
     * Returns a Flow emitting the single pet (ID = 0), mapping any null from
     * the database into a default PetEntity().
     */
    fun petFlow(): Flow<PetEntity> =
        petDao.getPetFlow(DEFAULT_ID)
            .map { dbEntity ->
                dbEntity ?: PetEntity(
                    id = DEFAULT_ID,
                    name = "Your Pet",
                    hungerLevel = DEFAULT_HUNGER,
                    health = DEFAULT_HEALTH,
                    happiness = DEFAULT_HAPPINESS
                )
            }

    /**
     * Returns the current hunger level (0–100), or DEFAULT_HUNGER if none in DB.
     * Needed for PetDecayWorker.
     */
    suspend fun getHunger(): Int =
        petDao.getPetOnce(DEFAULT_ID)?.hungerLevel ?: DEFAULT_HUNGER

    /** Loads the pet once or returns a default PetEntity if none exists. */
    private suspend fun loadPet(): PetEntity =
        petDao.getPetOnce(DEFAULT_ID) ?: PetEntity(
            id = DEFAULT_ID,
            name = "Your Pet",
            hungerLevel = DEFAULT_HUNGER,
            health = DEFAULT_HEALTH,
            happiness = DEFAULT_HAPPINESS
        )

    /** Sets the hunger level directly (clamped to 0–100). */
    suspend fun setHunger(level: Int) {
        val pet = loadPet().copy(
            hungerLevel = level.coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /** Changes the health by `delta` (clamped to 0–100). */
    suspend fun changeHealth(delta: Int) {
        val old = loadPet()
        val pet = old.copy(
            health = (old.health + delta).coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /** Changes the happiness by `delta` (clamped to 0–100). */
    suspend fun changeHappiness(delta: Int) {
        val old = loadPet()
        val pet = old.copy(
            happiness = (old.happiness + delta).coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /** Writes changes to the database and checks for critical levels. */
    private suspend fun upsertPet(pet: PetEntity) {
        petDao.upsert(pet)
        checkCritical(pet)
    }

    /**
     * If health or happiness reaches 0, handle the pet leaving or being sad.
     */
    private suspend fun checkCritical(pet: PetEntity) {
        if (pet.health == 0 || pet.happiness == 0) {
            // Example: delete the pet when it’s too sad or unhealthy
            petDao.delete(pet)
        }
    }
}





