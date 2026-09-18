package com.minimalphone.core.data.di

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.DefaultAppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.DefaultAppTimeLimitRepository
import com.minimalphone.core.data.repository.DefaultBlockedAttemptRepository
import com.minimalphone.core.data.repository.DefaultEssentialAppRepository
import com.minimalphone.core.data.repository.DefaultExitAttemptRepository
import com.minimalphone.core.data.repository.DefaultFocusSessionRepository
import com.minimalphone.core.data.repository.DefaultSettingsRepository
import com.minimalphone.core.data.repository.DefaultUsageStatsRepository
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.data.repository.ExitAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppRepository(
        impl: DefaultAppRepository
    ): AppRepository = impl

    @Provides
    @Singleton
    fun provideAppTimeLimitRepository(
        impl: DefaultAppTimeLimitRepository
    ): AppTimeLimitRepository = impl

    @Provides
    @Singleton
    fun provideUsageStatsRepository(
        impl: DefaultUsageStatsRepository
    ): UsageStatsRepository = impl

    @Provides
    @Singleton
    fun provideBlockedAttemptRepository(
        impl: DefaultBlockedAttemptRepository
    ): BlockedAttemptRepository = impl

    @Provides
    @Singleton
    fun provideFocusSessionRepository(
        impl: DefaultFocusSessionRepository
    ): FocusSessionRepository = impl

    @Provides
    @Singleton
    fun provideExitAttemptRepository(
        impl: DefaultExitAttemptRepository
    ): ExitAttemptRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(
        impl: DefaultSettingsRepository
    ): SettingsRepository = impl

    @Provides
    @Singleton
    fun provideEssentialAppRepository(
        impl: DefaultEssentialAppRepository
    ): EssentialAppRepository = impl

    @Provides
    @Singleton
    fun provideScheduleRepository(
        impl: com.minimalphone.core.data.repository.DefaultScheduleRepository
    ): com.minimalphone.core.data.repository.ScheduleRepository = impl

    @Provides
    @Singleton
    fun provideNotificationDigestRepository(
        impl: com.minimalphone.core.data.repository.DefaultNotificationDigestRepository
    ): com.minimalphone.core.data.repository.NotificationDigestRepository = impl

    @Provides
    @Singleton
    fun provideContactRepository(
        impl: com.minimalphone.core.data.repository.DefaultContactRepository
    ): com.minimalphone.core.data.repository.ContactRepository = impl
}

