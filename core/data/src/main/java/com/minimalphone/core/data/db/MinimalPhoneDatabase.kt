package com.minimalphone.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.minimalphone.core.data.db.converters.MinimalConverters
import com.minimalphone.core.data.db.dao.AppRuleDao
import com.minimalphone.core.data.db.dao.AppTimeLimitDao
import com.minimalphone.core.data.db.dao.BlockedAttemptDao
import com.minimalphone.core.data.db.dao.ExitAttemptDao
import com.minimalphone.core.data.db.dao.FocusSessionDao
import com.minimalphone.core.data.db.dao.InstalledAppDao
import com.minimalphone.core.data.db.entity.AppRuleEntity
import com.minimalphone.core.data.db.entity.AppTimeLimitEntity
import com.minimalphone.core.data.db.entity.BlockedAttemptEntity
import com.minimalphone.core.data.db.entity.ExitAttemptEntity
import com.minimalphone.core.data.db.entity.FocusGoalEntity
import com.minimalphone.core.data.db.entity.FocusSessionEntity
import com.minimalphone.core.data.db.entity.InstalledAppEntity

@Database(
    entities = [
        InstalledAppEntity::class,
        AppRuleEntity::class,
        FocusSessionEntity::class,
        BlockedAttemptEntity::class,
        ExitAttemptEntity::class,
        FocusGoalEntity::class,
        AppTimeLimitEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(MinimalConverters::class)
abstract class MinimalPhoneDatabase : RoomDatabase() {
    abstract fun installedAppDao(): InstalledAppDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun blockedAttemptDao(): BlockedAttemptDao
    abstract fun exitAttemptDao(): ExitAttemptDao
    abstract fun appTimeLimitDao(): AppTimeLimitDao

    companion object {
        const val DATABASE_NAME = "minimal_phone.db"
    }
}
