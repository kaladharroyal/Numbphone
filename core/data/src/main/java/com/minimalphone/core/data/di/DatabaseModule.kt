package com.minimalphone.core.data.di

import android.content.Context
import androidx.room.Room
import com.minimalphone.core.data.db.MinimalPhoneDatabase
import com.minimalphone.core.data.db.dao.AppRuleDao
import com.minimalphone.core.data.db.dao.BlockedAttemptDao
import com.minimalphone.core.data.db.dao.ExitAttemptDao
import com.minimalphone.core.data.db.dao.FocusSessionDao
import com.minimalphone.core.data.db.dao.InstalledAppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MinimalPhoneDatabase {
        return Room.databaseBuilder(
            context,
            MinimalPhoneDatabase::class.java,
            MinimalPhoneDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    fun provideInstalledAppDao(db: MinimalPhoneDatabase): InstalledAppDao = db.installedAppDao()

    @Provides
    fun provideAppRuleDao(db: MinimalPhoneDatabase): AppRuleDao = db.appRuleDao()

    @Provides
    fun provideFocusSessionDao(db: MinimalPhoneDatabase): FocusSessionDao = db.focusSessionDao()

    @Provides
    fun provideBlockedAttemptDao(db: MinimalPhoneDatabase): BlockedAttemptDao = db.blockedAttemptDao()

    @Provides
    fun provideExitAttemptDao(db: MinimalPhoneDatabase): ExitAttemptDao = db.exitAttemptDao()

    @Provides
    fun provideAppTimeLimitDao(db: MinimalPhoneDatabase): com.minimalphone.core.data.db.dao.AppTimeLimitDao = db.appTimeLimitDao()
}
