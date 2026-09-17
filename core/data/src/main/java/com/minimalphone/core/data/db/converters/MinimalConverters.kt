package com.minimalphone.core.data.db.converters

import androidx.room.TypeConverter
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.ClassificationSource
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.SessionStatus

class MinimalConverters {

    @TypeConverter
    fun fromAppCategory(category: AppCategory): String = category.name

    @TypeConverter
    fun toAppCategory(value: String): AppCategory = try {
        AppCategory.valueOf(value)
    } catch (e: Exception) {
        AppCategory.MANAGED
    }

    @TypeConverter
    fun fromClassificationSource(source: ClassificationSource): String = source.name

    @TypeConverter
    fun toClassificationSource(value: String): ClassificationSource = try {
        ClassificationSource.valueOf(value)
    } catch (e: Exception) {
        ClassificationSource.AUTOMATIC_DEFAULT
    }

    @TypeConverter
    fun fromFocusMode(mode: FocusMode): String = mode.name

    @TypeConverter
    fun toFocusMode(value: String): FocusMode = try {
        FocusMode.valueOf(value)
    } catch (e: Exception) {
        FocusMode.STRICT
    }

    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = try {
        SessionStatus.valueOf(value)
    } catch (e: Exception) {
        SessionStatus.ACTIVE
    }
}
