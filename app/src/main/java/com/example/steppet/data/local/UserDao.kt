package com.example.steppet.data.local

import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :u AND password = :p LIMIT 1")
    suspend fun findUser(u: String, p: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserEntity>

    /** Aktualisiert den Nutzernamen */
    @Query("UPDATE users SET username = :newName WHERE username = :oldName")
    suspend fun updateUsername(oldName: String, newName: String): Int

    /** Löscht den Account */
    @Query("DELETE FROM users WHERE username = :u")
    suspend fun deleteUser(u: String): Int
}
