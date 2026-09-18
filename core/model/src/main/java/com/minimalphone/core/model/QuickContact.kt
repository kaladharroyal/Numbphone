package com.minimalphone.core.model

/**
 * Domain model for a pinned quick-dial contact on the Minimal Phone launcher.
 */
data class QuickContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isPinned: Boolean = true
)
