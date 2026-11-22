package com.minimalphone.core.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class Migration8To9Test {

    @Test
    fun `migration 8 to 9 executes alter table for app_rules and installed_apps`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        MinimalPhoneDatabase.MIGRATION_8_9.migrate(db)

        verify { db.execSQL("ALTER TABLE app_rules ADD COLUMN familyId TEXT DEFAULT NULL") }
        verify { db.execSQL("ALTER TABLE app_rules ADD COLUMN quarantineUntil INTEGER DEFAULT NULL") }
        verify { db.execSQL("ALTER TABLE installed_apps ADD COLUMN familyId TEXT DEFAULT NULL") }
        verify { db.execSQL("ALTER TABLE installed_apps ADD COLUMN quarantineUntil INTEGER DEFAULT NULL") }
        verify { db.execSQL("ALTER TABLE installed_apps ADD COLUMN isClone INTEGER NOT NULL DEFAULT 0") }
        verify { db.execSQL("ALTER TABLE exit_attempts ADD COLUMN route TEXT NOT NULL DEFAULT 'FOCUS_SESSION'") }
    }
}
