package com.example.steppet.data.local

import androidx.room.*

@Dao
interface UserDao {
    /** Returns the single (or null) saved user row. */
    @Query("SELECT * FROM users WHERE id = 0")
    suspend fun getUser(): UserEntity?

    /** Insert or replace that one row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    /** Clears the table entirely (logs out). */
    @Query("DELETE FROM users")
    suspend fun clearAll()
}
