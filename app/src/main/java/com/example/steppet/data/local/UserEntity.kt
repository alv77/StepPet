package com.example.steppet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding saved login credentials.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int = 0,
    val username: String,
    val password: String
)
