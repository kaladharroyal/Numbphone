package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.QuickContactDao
import com.minimalphone.core.data.db.entity.QuickContactEntity
import com.minimalphone.core.model.QuickContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultContactRepository @Inject constructor(
    private val contactDao: QuickContactDao
) : ContactRepository {

    override fun observePinnedContacts(): Flow<List<QuickContact>> {
        return contactDao.observePinnedContacts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAllContacts(): Flow<List<QuickContact>> {
        return contactDao.observeAllContacts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveContact(name: String, phoneNumber: String, isPinned: Boolean) {
        val id = "contact_${UUID.randomUUID()}"
        contactDao.insert(
            QuickContactEntity(
                id = id,
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                isPinned = isPinned
            )
        )
    }

    override suspend fun deleteContact(id: String) {
        contactDao.delete(id)
    }

    private fun QuickContactEntity.toDomain() = QuickContact(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        isPinned = isPinned
    )
}
