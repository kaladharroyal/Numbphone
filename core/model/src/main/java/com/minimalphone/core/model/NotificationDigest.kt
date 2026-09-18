package com.minimalphone.core.model

/**
 * Domain model for a bundled notification in the distraction-free Notification Digest.
 */
data class DigestNotification(
    val id: Long = 0L,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
