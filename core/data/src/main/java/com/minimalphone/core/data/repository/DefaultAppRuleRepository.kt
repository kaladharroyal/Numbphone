package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.AppRuleDao
import com.minimalphone.core.data.db.entity.AppRuleEntity
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppRule
import com.minimalphone.core.model.ClassificationSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAppRuleRepository @Inject constructor(
    private val appRuleDao: AppRuleDao
) : AppRuleRepository {

    override suspend fun getQuarantineUntil(packageName: String): Long? {
        return appRuleDao.getRule(packageName)?.quarantineUntil
    }

    override suspend fun setQuarantineUntil(packageName: String, until: Long?) {
        val existing = appRuleDao.getRule(packageName)
        val entity = existing?.copy(quarantineUntil = until)
            ?: AppRuleEntity(
                packageName = packageName,
                category = AppCategory.MANAGED,
                isAlwaysAvailable = false,
                isBlockedInFocus = true,
                isFavoriteOnHome = false,
                classificationSource = ClassificationSource.AUTOMATIC_DEFAULT,
                quarantineUntil = until
            )
        appRuleDao.upsertRule(entity)
    }

    override suspend fun getFamilyId(packageName: String): String? {
        return appRuleDao.getRule(packageName)?.familyId
    }

    override suspend fun getByFamily(familyId: String): List<AppRule> {
        return appRuleDao.getByFamily(familyId).map { it.toModel() }
    }

    override suspend fun clearExpiredQuarantines() {
        appRuleDao.clearExpiredQuarantines(System.currentTimeMillis())
    }

    override fun observeQuarantinedCount(): Flow<Int> {
        return appRuleDao.observeQuarantinedCount(System.currentTimeMillis())
    }

    override suspend fun getRule(packageName: String): AppRule? {
        return appRuleDao.getRule(packageName)?.toModel()
    }

    override suspend fun saveRule(rule: AppRule) {
        appRuleDao.upsertRule(
            AppRuleEntity(
                packageName = rule.packageName,
                category = rule.category,
                isAlwaysAvailable = rule.isAlwaysAvailable,
                isBlockedInFocus = rule.isBlockedInFocus,
                isFavoriteOnHome = false,
                customLabel = rule.customLabel,
                classificationSource = rule.classificationSource,
                updatedTimestamp = rule.updatedTimestamp,
                familyId = rule.familyId,
                quarantineUntil = rule.quarantineUntil
            )
        )
    }

    override fun observeAllRules(): Flow<List<AppRule>> {
        return appRuleDao.observeAllRules().map { list -> list.map { it.toModel() } }
    }

    override suspend fun getCustomLabel(packageName: String): String? {
        return appRuleDao.getRule(packageName)?.customLabel
    }

    private fun AppRuleEntity.toModel(): AppRule = AppRule(
        packageName = packageName,
        category = category,
        isAlwaysAvailable = isAlwaysAvailable,
        isBlockedInFocus = isBlockedInFocus,
        customLabel = customLabel,
        classificationSource = classificationSource,
        updatedTimestamp = updatedTimestamp,
        familyId = familyId,
        quarantineUntil = quarantineUntil
    )
}
