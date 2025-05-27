package com.example.steppet.data.local

import androidx.room.*

@Dao
interface UserDao {
    /** Find a user by exact username+password. */
    @Query("SELECT * FROM users WHERE username = :u AND password = :p LIMIT 1")
    suspend fun findUser(u: String, p: String): UserEntity?

    /** Insert a new user, fail if username already exists. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    /** Optionally: list all users (for debugging). */
    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserEntity>
}
