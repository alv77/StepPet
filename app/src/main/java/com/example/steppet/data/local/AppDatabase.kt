package com.example.steppet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main Room database for the StepPet app.
 * Wir haben die Version hochgesetzt, weil wir Felder in PetEntity hinzugefügt haben.
 * Mit fallbackToDestructiveMigration(true) wird bei Versionssprung die alte DB gelöscht.
 */
@Database(
    entities = [UserEntity::class, PetEntity::class],
    version = 3,                    // Version von 2 auf 3 erhöht
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of AppDatabase.
         * Mit fallbackToDestructiveMigration(true) löschen wir die DB bei Versionswechsel.
         */
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "step_pet_db"
            )
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
