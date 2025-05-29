package com.example.steppet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the pet’s status.
 */
@Entity(tableName = "pet")
data class PetEntity(
    @PrimaryKey val id: Long = 0L,

    /** Display name of your pet */
    val name: String = "Your Pet",

    /** Hunger level in range 0–100 */
    val hungerLevel: Int = 100,

    /** Health level in range 0–100 */
    val health: Int = 100,

    /** Happiness level in range 0–100 */
    val happiness: Int = 100
)


