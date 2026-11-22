package com.minimalphone.core.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class Migration9To10Test {

    @Test
    fun `migration 9 to 10 creates app_pause_configs and pause_events tables`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        MinimalPhoneDatabase.MIGRATION_9_10.migrate(db)

        verify {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `app_pause_configs`") })
        }
        verify {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `pause_events`") })
        }
    }
}
