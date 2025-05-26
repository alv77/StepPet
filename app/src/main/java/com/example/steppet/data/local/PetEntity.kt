package com.example.steppet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the pet’s hunger level.
 */
@Entity(tableName = "pet")
data class PetEntity(
    @PrimaryKey val id: Int = 0,
    val hungerLevel: Int
)
