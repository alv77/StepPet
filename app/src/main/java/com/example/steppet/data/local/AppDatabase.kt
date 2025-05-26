package com.example.steppet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, PetEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(ctx: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDatabase::class.java,
                    "steppet.db"
                ).build().also { INSTANCE = it }
            }
    }
}
