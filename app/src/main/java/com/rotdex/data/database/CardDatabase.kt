package com.rotdex.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rotdex.data.models.AchievementProgress
import com.rotdex.data.models.Card
import com.rotdex.data.models.FusionHistory
import com.rotdex.data.models.SpinHistory
import com.rotdex.data.models.UserProfile

/**
 * Room database for storing brainrot cards and user data
 */
@Database(
    entities = [Card::class, UserProfile::class, SpinHistory::class, FusionHistory::class, AchievementProgress::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CardDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun spinHistoryDao(): SpinHistoryDao
    abstract fun fusionHistoryDao(): FusionHistoryDao
    abstract fun achievementDao(): AchievementDao

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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
