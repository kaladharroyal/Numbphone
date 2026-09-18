package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.model.AppTimeLimit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppTimeLimitsUseCase @Inject constructor(
    private val appTimeLimitRepository: AppTimeLimitRepository
) {
    operator fun invoke(): Flow<List<AppTimeLimit>> {
        return appTimeLimitRepository.observeAllLimits()
    }
}

class GetAppTimeLimitUseCase @Inject constructor(
    private val appTimeLimitRepository: AppTimeLimitRepository
) {
    operator fun invoke(packageName: String): Flow<AppTimeLimit?> {
        return appTimeLimitRepository.observeLimit(packageName)
    }

    suspend fun getSync(packageName: String): AppTimeLimit? {
        return appTimeLimitRepository.getLimit(packageName)
    }
}

class SetAppTimeLimitUseCase @Inject constructor(
    private val appTimeLimitRepository: AppTimeLimitRepository
) {
    suspend operator fun invoke(packageName: String, limitMinutes: Int, isEnabled: Boolean = true) {
        appTimeLimitRepository.setLimit(packageName, limitMinutes, isEnabled)
    }
}

class RemoveAppTimeLimitUseCase @Inject constructor(
    private val appTimeLimitRepository: AppTimeLimitRepository
) {
    suspend operator fun invoke(packageName: String) {
        appTimeLimitRepository.removeLimit(packageName)
    }
}

class ExtendAppTimeLimitUseCase @Inject constructor(
    private val appTimeLimitRepository: AppTimeLimitRepository
) {
    suspend operator fun invoke(packageName: String, additionalMinutes: Int) {
        if (additionalMinutes > 0) {
            appTimeLimitRepository.addEmergencyExtension(packageName, additionalMinutes)
        }
    }
}
