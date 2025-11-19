package com.rotdex.di

import android.content.Context
import com.rotdex.data.api.AiApiService
import com.rotdex.data.database.CardDao
import com.rotdex.data.database.FusionHistoryDao
import com.rotdex.data.database.SpinHistoryDao
import com.rotdex.data.database.UserProfileDao
import com.rotdex.data.repository.CardRepository
import com.rotdex.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCardRepository(
        @ApplicationContext context: Context,
        cardDao: CardDao,
        fusionHistoryDao: FusionHistoryDao,
        aiApiService: AiApiService,
        userRepository: UserRepository
    ): CardRepository {
        return CardRepository(context, cardDao, fusionHistoryDao, aiApiService, userRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userProfileDao: UserProfileDao,
        spinHistoryDao: SpinHistoryDao
    ): UserRepository {
        return UserRepository(userProfileDao, spinHistoryDao)
    }
}
