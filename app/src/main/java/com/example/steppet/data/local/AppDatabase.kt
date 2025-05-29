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
    version = 2,                    // Schema-Version erhöht
    exportSchema = false             // Schema in JSON exportieren
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of AppDatabase.
         * Applies destructive migration on version mismatch.
         */
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "step_pet_db"              // einheitlicher Name
            )
                .fallbackToDestructiveMigration()  // Drop & recreate on schema change
                .build()
    }
}

