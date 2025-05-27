package com.example.steppet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents one registered user.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val password: String
)
