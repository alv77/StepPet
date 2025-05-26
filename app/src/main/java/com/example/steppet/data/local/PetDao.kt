package com.example.steppet.data.local

import androidx.room.*

@Dao
interface PetDao {
    @Query("SELECT * FROM pet WHERE id = 0")
    suspend fun getPet(): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pet: PetEntity)
}
