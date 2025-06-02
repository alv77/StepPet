package com.example.steppet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the pet’s status.
 *
 * Neue Felder:
 *  • feedsDoneToday: Wie oft heute schon gefüttert wurde (0–10)
 *  • lastFeedDate: ISO-Datum (yyyy-MM-dd), an dem feedsDoneToday zuletzt zurückgesetzt wurde
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
    val happiness: Int = 100,

    /** Wie oft heute schon gefüttert wurde (0–10) */
    val feedsDoneToday: Int = 0,

    /** ISO-Datum (yyyy-MM-dd), an dem feedsDoneToday zuletzt zurückgesetzt wurde */
    val lastFeedDate: String = ""
)
