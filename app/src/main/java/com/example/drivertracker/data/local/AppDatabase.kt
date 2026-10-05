package com.example.drivertracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.drivertracker.data.local.dao.OrderDao
import com.example.drivertracker.data.local.entity.OrderRecord

@Database(entities = [OrderRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "driver_tracker_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
