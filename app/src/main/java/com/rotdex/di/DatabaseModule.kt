package com.rotdex.di

import android.content.Context
import com.rotdex.data.database.CardDao
import com.rotdex.data.database.CardDatabase
import com.rotdex.data.database.FusionHistoryDao
import com.rotdex.data.database.SpinHistoryDao
import com.rotdex.data.database.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCardDatabase(
        @ApplicationContext context: Context
    ): CardDatabase {
        return CardDatabase.getDatabase(context)
    }

    @Provides
    fun provideCardDao(database: CardDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    fun provideUserProfileDao(database: CardDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    fun provideSpinHistoryDao(database: CardDatabase): SpinHistoryDao {
        return database.spinHistoryDao()
    }

    @Provides
    fun provideFusionHistoryDao(database: CardDatabase): FusionHistoryDao {
        return database.fusionHistoryDao()
    }
}
