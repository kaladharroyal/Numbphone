package com.minimalphone.core.domain.pause

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class PauseGrant(
    val packageName: String,
    val grantElapsedMs: Long,
    val expiryElapsedMs: Long,
    val sessionMinutes: Int,
    @Volatile var lastActiveElapsedMs: Long
)

@Singleton
class PauseGrantStore @Inject constructor() {

    private var clock: () -> Long = { SystemClock.elapsedRealtime() }
    private val grants = ConcurrentHashMap<String, PauseGrant>()

    constructor(customClock: () -> Long) : this() {
        this.clock = customClock
    }

    fun setClock(customClock: () -> Long) {
        this.clock = customClock
    }

    /**
     * Records a grant valid for [sessionMinutes].
     * If [familyPackages] is provided, grants are mirrored across the sibling apps.
     */
    fun grant(
        packageName: String,
        sessionMinutes: Int,
        familyPackages: List<String> = emptyList()
    ) {
        val now = clock()
        val expiry = now + (sessionMinutes * 60 * 1000L)
        val targets = if (familyPackages.isNotEmpty()) familyPackages else listOf(packageName)
        for (pkg in targets) {
            grants[pkg] = PauseGrant(
                packageName = pkg,
                grantElapsedMs = now,
                expiryElapsedMs = expiry,
                sessionMinutes = sessionMinutes,
                lastActiveElapsedMs = now
            )
        }
    }

    /**
     * Returns true if a valid grant exists and the exit grace period (30s) has not lapsed.
     */
    fun hasValidGrant(packageName: String): Boolean {
        val now = clock()
        val grant = grants[packageName] ?: return false

        // Check overall session expiry
        if (now >= grant.expiryElapsedMs) {
            grants.remove(packageName)
            return false
        }

        // Check exit grace (away > 30s)
        if (now - grant.lastActiveElapsedMs > EXIT_GRACE_MS) {
            grants.remove(packageName)
            return false
        }

        return true
    }

    /**
     * Called when the app transitions to the foreground.
     */
    fun onAppForeground(packageName: String) {
        val now = clock()
        val grant = grants[packageName]
        if (grant != null) {
            if (now < grant.expiryElapsedMs && (now - grant.lastActiveElapsedMs <= EXIT_GRACE_MS)) {
                grant.lastActiveElapsedMs = now
            } else {
                grants.remove(packageName)
            }
        }
    }

    /**
     * Called when the app leaves the foreground.
     */
    fun onAppBackground(packageName: String) {
        val now = clock()
        val grant = grants[packageName]
        if (grant != null) {
            grant.lastActiveElapsedMs = now
        }
    }

    /**
     * Returns remaining grant time in milliseconds.
     */
    fun remainingMillis(packageName: String): Long {
        val now = clock()
        val grant = grants[packageName] ?: return 0L
        if (!hasValidGrant(packageName)) return 0L
        return (grant.expiryElapsedMs - now).coerceAtLeast(0L)
    }

    fun revokeGrant(packageName: String) {
        grants.remove(packageName)
    }

    fun clear() {
        grants.clear()
    }

    companion object {
        const val EXIT_GRACE_MS = 30_000L
    }
}
