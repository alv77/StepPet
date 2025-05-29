package com.example.steppet.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface PetDao {

    /**
     * Streams the single row in table `pet` (ID = 0).
     * Returns a nullable entity so we can distinguish “keine Zeile”
     * und im Repository einen Default-Pet daraus machen.
     */
    @Query("SELECT * FROM pet WHERE id = :id")
    fun getPetFlow(id: Long = 0L): Flow<PetEntity?>

    /** Reads the single row once, or null if not present. */
    @Query("SELECT * FROM pet WHERE id = :id")
    suspend fun getPetOnce(id: Long = 0L): PetEntity?

    /** Insert or update on conflict. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pet: PetEntity)

    /** Delete the row. */
    @Delete
    suspend fun delete(pet: PetEntity)

    /** Convenience to change health by delta. */
    @Transaction
    suspend fun updateHealth(id: Long, delta: Int) {
        val pet = getPetOnce(id)
        pet?.let {
            val newHealth = (it.health + delta).coerceIn(0, 100)
            upsert(it.copy(health = newHealth))
        }
    }

    /** Convenience to change happiness by delta. */
    @Transaction
    suspend fun updateHappiness(id: Long, delta: Int) {
        val pet = getPetOnce(id)
        pet?.let {
            val newHappiness = (it.happiness + delta).coerceIn(0, 100)
            upsert(it.copy(happiness = newHappiness))
        }
    }
}



