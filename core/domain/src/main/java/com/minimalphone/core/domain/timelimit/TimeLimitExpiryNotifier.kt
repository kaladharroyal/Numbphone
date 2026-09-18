package com.minimalphone.core.domain.timelimit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ExpiredAppEvent(
    val packageName: String,
    val appLabel: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class TimeLimitExpiryNotifier @Inject constructor() {
    private val _currentExpiredEvent = MutableStateFlow<ExpiredAppEvent?>(null)
    val currentExpiredEvent: StateFlow<ExpiredAppEvent?> = _currentExpiredEvent.asStateFlow()

    fun notifyTimeLimitExpired(packageName: String, appLabel: String) {
        _currentExpiredEvent.value = ExpiredAppEvent(packageName, appLabel)
    }

    fun clearExpiredEvent() {
        _currentExpiredEvent.value = null
    }
}

