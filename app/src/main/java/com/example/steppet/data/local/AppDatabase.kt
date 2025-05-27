package com.example.steppet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main Room database for the StepPet app.
 * Uses destructive migrations to wipe & rebuild whenever the schema changes.
 */
@Database(
    entities = [UserEntity::class, PetEntity::class],
    version = 2,                     // ← bump from 1 → 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(ctx: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDatabase::class.java,
                    "steppet.db"
                )
                    // Drop & recreate the database on any version/schema mismatch
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
