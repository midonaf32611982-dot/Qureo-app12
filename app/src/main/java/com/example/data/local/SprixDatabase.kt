package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ActivationCodeEntity
import com.example.data.model.ActivityLogEntity
import com.example.data.model.SolverHistoryEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ActivationCodeEntity::class,
        ActivityLogEntity::class,
        SolverHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SprixDatabase : RoomDatabase() {
    abstract fun sprixDao(): SprixDao

    companion object {
        @Volatile
        private var INSTANCE: SprixDatabase? = null

        fun getDatabase(context: Context): SprixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SprixDatabase::class.java,
                    "sprix_solver_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
