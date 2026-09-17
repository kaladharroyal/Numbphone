package com.minimalphone.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.minimalphone.MainActivity
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.domain.bypass.EvaluateBypassRouteUseCase
import com.minimalphone.core.domain.bypass.RecordBypassAttemptUseCase
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.BypassDecision
import com.minimalphone.core.model.BypassRoute
import com.minimalphone.core.model.FocusMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MinimalAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var evaluateBypassRouteUseCase: EvaluateBypassRouteUseCase

    @Inject
    lateinit var recordBypassAttemptUseCase: RecordBypassAttemptUseCase

    @Inject
    lateinit var focusSessionRepository: FocusSessionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Debounce state to avoid redundant checks during rapid window transitions
    private var lastCheckedPackage: String? = null
    private var lastCheckedTime: Long = 0L

    companion object {
        private const val TAG = "BypassAccessibilityService"
        private const val DEBOUNCE_WINDOW_MS = 350L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        MinimalLog.i(TAG, "Minimal Phone Bypass Protection Service Connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val targetPackage = event.packageName?.toString() ?: return
        if (targetPackage.isBlank()) return

        val className = event.className?.toString() ?: ""

        val currentTime = System.currentTimeMillis()
        if (targetPackage == lastCheckedPackage && className == lastCheckedClass && (currentTime - lastCheckedTime) < DEBOUNCE_WINDOW_MS) {
            return
        }

        lastCheckedPackage = targetPackage
        lastCheckedClass = className
        lastCheckedTime = currentTime

        // Fast exclusion: Skip Minimal Phone itself to prevent redirect loops
        if (targetPackage == packageName || targetPackage.startsWith("com.minimalphone")) {
            return
        }

        // Intercept System UI Recents Overview / Quick Settings / Notification Shade
        if (isSystemUiOrRecents(targetPackage, className)) {
            serviceScope.launch {
                try {
                    val activeSession = focusSessionRepository.getActiveSessionSync()
                    if (activeSession != null && activeSession.isCurrentlyActive &&
                        (activeSession.mode == FocusMode.STRICT || activeSession.mode == FocusMode.DEEP_FOCUS)
                    ) {
                        MinimalLog.w(TAG, "Blocked System UI Recents / Notification shade during focus (${activeSession.mode}). Dismissing.")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                        }
                        returnToHomeScreen()
                        recordBypassAttemptUseCase(
                            packageName = targetPackage,
                            route = BypassRoute.NOTIFICATION
                        )
                    }
                } catch (e: Exception) {
                    MinimalLog.e(TAG, "Error intercepting system UI", e)
                }
            }
            return
        }

        serviceScope.launch {
            try {
                when (val decision = evaluateBypassRouteUseCase(targetPackage)) {
                    is BypassDecision.Allow -> {
                        // Normal allowed execution; do nothing
                    }
                    is BypassDecision.InterceptAndRedirect -> {
                        MinimalLog.w(
                            TAG,
                            "Unauthorized access to '$targetPackage' during active focus session '${decision.session.id}' (${decision.session.mode}). Intercepting."
                        )

                        // 1. Record bypass attempt
                        recordBypassAttemptUseCase(
                            packageName = targetPackage,
                            route = BypassRoute.RECENTS
                        )

                        // 2. Perform redirect to Minimal Phone Home Screen
                        returnToHomeScreen()
                    }
                }
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Error evaluating bypass route for package: $targetPackage", e)
            }
        }
    }

    private var lastCheckedClass: String? = null

    private fun isSystemUiOrRecents(pkg: String, className: String): Boolean {
        val lowerClass = className.lowercase()
        val lowerPkg = pkg.lowercase()
        return lowerPkg.contains("systemui") ||
               lowerPkg.contains("nexuslauncher") ||
               lowerPkg.contains("quickstep") ||
               lowerClass.contains("recents") ||
               lowerClass.contains("overview") ||
               lowerClass.contains("notificationshade") ||
               lowerClass.contains("notificationpanel") ||
               lowerClass.contains("quicksettings")
    }

    private fun returnToHomeScreen() {
        // First try standard Global Action Home
        val homePerformed = performGlobalAction(GLOBAL_ACTION_HOME)
        if (!homePerformed) {
            // Fallback: Launch MainActivity directly
            val homeIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(homeIntent)
        }
    }

    override fun onInterrupt() {
        MinimalLog.w(TAG, "Accessibility service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        MinimalLog.i(TAG, "Minimal Phone Bypass Protection Service Destroyed.")
    }
}
