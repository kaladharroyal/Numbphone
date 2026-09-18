package com.minimalphone.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.minimalphone.core.data.db.converters.MinimalConverters
import com.minimalphone.core.data.db.dao.AppBudgetDao
import com.minimalphone.core.data.db.dao.AppRuleDao
import com.minimalphone.core.data.db.dao.BlockedAttemptDao
import com.minimalphone.core.data.db.dao.DailyAppUsageDao
import com.minimalphone.core.data.db.dao.DigestNotificationDao
import com.minimalphone.core.data.db.dao.EssentialAppDao
import com.minimalphone.core.data.db.dao.ExitAttemptDao
import com.minimalphone.core.data.db.dao.FocusPresetDao
import com.minimalphone.core.data.db.dao.FocusScheduleDao
import com.minimalphone.core.data.db.dao.FocusSessionDao
import com.minimalphone.core.data.db.dao.InstalledAppDao
import com.minimalphone.core.data.db.dao.QuickContactDao
import com.minimalphone.core.data.db.entity.AppBudgetEntity
import com.minimalphone.core.data.db.entity.AppRuleEntity
import com.minimalphone.core.data.db.entity.BlockedAttemptEntity
import com.minimalphone.core.data.db.entity.DailyAppUsageEntity
import com.minimalphone.core.data.db.entity.DigestNotificationEntity
import com.minimalphone.core.data.db.entity.EssentialAppEntity
import com.minimalphone.core.data.db.entity.ExitAttemptEntity
import com.minimalphone.core.data.db.entity.FocusGoalEntity
import com.minimalphone.core.data.db.entity.FocusPresetEntity
import com.minimalphone.core.data.db.entity.FocusScheduleEntity
import com.minimalphone.core.data.db.entity.FocusSessionEntity
import com.minimalphone.core.data.db.entity.InstalledAppEntity
import com.minimalphone.core.data.db.entity.QuickContactEntity

@Database(
    entities = [
        InstalledAppEntity::class,
        AppRuleEntity::class,
        FocusSessionEntity::class,
        BlockedAttemptEntity::class,
        ExitAttemptEntity::class,
        FocusGoalEntity::class,
        EssentialAppEntity::class,
        AppBudgetEntity::class,
        DailyAppUsageEntity::class,
        FocusPresetEntity::class,
        FocusScheduleEntity::class,
        DigestNotificationEntity::class,
        QuickContactEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(MinimalConverters::class)
abstract class MinimalPhoneDatabase : RoomDatabase() {
    abstract fun installedAppDao(): InstalledAppDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun blockedAttemptDao(): BlockedAttemptDao
    abstract fun exitAttemptDao(): ExitAttemptDao
    abstract fun essentialAppDao(): EssentialAppDao
    abstract fun appBudgetDao(): AppBudgetDao
    abstract fun dailyAppUsageDao(): DailyAppUsageDao
    abstract fun focusPresetDao(): FocusPresetDao
    abstract fun focusScheduleDao(): FocusScheduleDao
    abstract fun digestNotificationDao(): DigestNotificationDao
    abstract fun quickContactDao(): QuickContactDao

    companion object {
        const val DATABASE_NAME = "minimal_phone.db"
    }
}
