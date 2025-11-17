package com.rotdex.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rotdex.data.models.Card
import com.rotdex.data.models.UserProfile
import com.rotdex.data.models.SpinHistory

/**
 * Room database for storing brainrot cards and user data
 */
@Database(
    entities = [Card::class, UserProfile::class, SpinHistory::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CardDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun spinHistoryDao(): SpinHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CardDatabase? = null

        fun getDatabase(context: Context): CardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CardDatabase::class.java,
                    "rotdex_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
