package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.EssentialAppDao
import com.minimalphone.core.data.db.entity.EssentialAppEntity
import com.minimalphone.core.model.EssentialApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEssentialAppRepository @Inject constructor(
    private val essentialAppDao: EssentialAppDao
) : EssentialAppRepository {

    override fun observeEssentialApps(): Flow<List<EssentialApp>> =
        essentialAppDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getEssentialPackages(): Set<String> =
        essentialAppDao.getAllPackageNames().toSet()

    override fun observeEssentialPackages(): Flow<Set<String>> =
        essentialAppDao.observeAll().map { entities ->
            entities.map { it.packageName }.toSet()
        }

    override suspend fun addEssentialApp(
        packageName: String,
        label: String,
        isSystemDefault: Boolean
    ) {
        essentialAppDao.upsert(
            EssentialAppEntity(
                packageName = packageName,
                label = label,
                isSystemDefault = isSystemDefault
            )
        )
    }

    override suspend fun removeEssentialApp(packageName: String) {
        // Only removes user-added entries; isSystemDefault = 1 rows are protected by the DAO query
        essentialAppDao.deleteUserAdded(packageName)
    }

    override suspend fun seedDefaults(apps: List<Pair<String, String>>) {
        val entities = apps.map { (pkg, label) ->
            EssentialAppEntity(
                packageName = pkg,
                label = label,
                isSystemDefault = true
            )
        }
        essentialAppDao.upsertAll(entities)
    }

    private fun EssentialAppEntity.toDomain() = EssentialApp(
        packageName = packageName,
        label = label,
        isSystemDefault = isSystemDefault,
        addedTimestamp = addedTimestamp
    )
}
