package com.minimalphone.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.minimalphone.MainActivity
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.bypass.EvaluateBypassRouteUseCase
import com.minimalphone.core.domain.bypass.RecordBypassAttemptUseCase
import com.minimalphone.core.domain.timelimit.TimeLimitExpiryNotifier
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.BypassDecision
import com.minimalphone.core.model.BypassRoute
import com.minimalphone.core.model.FocusMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var appTimeLimitRepository: AppTimeLimitRepository

    @Inject
    lateinit var usageStatsRepository: UsageStatsRepository

    @Inject
    lateinit var timeLimitExpiryNotifier: TimeLimitExpiryNotifier

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Debounce state to avoid redundant checks during rapid window transitions
    private var lastCheckedPackage: String? = null
    private var lastCheckedClass: String? = null
    private var lastCheckedTime: Long = 0L

    // Active foreground tracking job
    private var activeMonitoringJob: Job? = null
    private var currentForegroundPackage: String? = null

    companion object {
        private const val TAG = "BypassAccessibilityService"
        private const val DEBOUNCE_WINDOW_MS = 250L
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

        // 1. Fast exclusion: Skip Minimal Phone itself to prevent redirect loops
        if (targetPackage == packageName || targetPackage.startsWith("com.minimalphone")) {
            stopMonitoring()
            return
        }

        // 2. Ignore transient system overlays, keyboards, input methods, and frameworks
        // (Do NOT stop monitoring the active user app underneath!)
        if (isSystemPackageWhitelisted(targetPackage)) {
            return
        }

        // 3. Intercept System UI Recents Overview / Quick Settings / Notification Shade during active focus
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

        // 4. Evaluate bypass & time limit rules
        serviceScope.launch {
            try {
                when (val decision = evaluateBypassRouteUseCase(targetPackage)) {
                    is BypassDecision.Allow -> {
                        startMonitoringForegroundApp(targetPackage)
                    }
                    is BypassDecision.InterceptAndRedirect -> {
                        stopMonitoring()
                        val isLimitExpired = decision.session.id.startsWith("limit_") ||
                                decision.session.goal.title == "Daily Limit Reached"

                        MinimalLog.w(
                            TAG,
                            "Unauthorized access to '$targetPackage' (${if (isLimitExpired) "Daily Limit Reached" else "Focus Session"}). Intercepting."
                        )

                        // 1. Record bypass attempt
                        recordBypassAttemptUseCase(
                            packageName = targetPackage,
                            route = BypassRoute.RECENTS,
                            userIntentReason = if (isLimitExpired) "Daily limit reached" else "Focus active"
                        )

                        // 2. Perform redirect
                        if (isLimitExpired) {
                            val app = appRepository.getApp(targetPackage)
                            val label = app?.label ?: targetPackage.substringAfterLast('.')
                            timeLimitExpiryNotifier.notifyTimeLimitExpired(targetPackage, label)
                            returnToHomeScreenWithExpiry(targetPackage, label)
                        } else {
                            returnToHomeScreen()
                        }
                    }
                }
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Error evaluating bypass route for package: $targetPackage", e)
            }
        }
    }

    private fun startMonitoringForegroundApp(targetPackage: String) {
        if (currentForegroundPackage == targetPackage && activeMonitoringJob?.isActive == true) {
            return
        }

        stopMonitoring()
        currentForegroundPackage = targetPackage

        activeMonitoringJob = serviceScope.launch {
            try {
                val app = appRepository.getApp(targetPackage)
                val appLabel = app?.label ?: targetPackage.substringAfterLast('.')
                val isEssential = app?.isEssential == true || app?.category == AppCategory.ESSENTIAL

                val baseUsageMinutes = usageStatsRepository.getTodayUsageMinutes(targetPackage)
                val foregroundStartTime = System.currentTimeMillis()

                MinimalLog.i(TAG, "Started real-time monitoring for $targetPackage (Base usage: ${baseUsageMinutes}m)")

                while (isActive) {
                    delay(1000L)

                    // 1. Monitor Daily Time Limit in Real-Time (Checked every second)
                    val timeLimit = appTimeLimitRepository.getLimit(targetPackage)
                    if (timeLimit != null && timeLimit.isEnabled && timeLimit.effectiveDailyLimitMinutes > 0) {
                        val elapsedForegroundSeconds = (System.currentTimeMillis() - foregroundStartTime) / 1000L
                        val totalUsedSeconds = (baseUsageMinutes * 60L) + elapsedForegroundSeconds
                        val limitSeconds = timeLimit.effectiveDailyLimitMinutes * 60L

                        if (totalUsedSeconds >= limitSeconds) {
                            MinimalLog.w(
                                TAG,
                                "Real-time daily limit reached for $targetPackage ($totalUsedSeconds s >= $limitSeconds s). Closing app."
                            )
                            recordBypassAttemptUseCase(
                                packageName = targetPackage,
                                route = BypassRoute.RECENTS,
                                userIntentReason = "Daily limit reached"
                            )
                            timeLimitExpiryNotifier.notifyTimeLimitExpired(targetPackage, appLabel)
                            returnToHomeScreenWithExpiry(targetPackage, appLabel)
                            stopMonitoring()
                            break
                        }
                    }

                    // 2. Monitor Active Focus Session starting while in an app
                    if (!isEssential) {
                        val activeSession = focusSessionRepository.getActiveSessionSync()
                        if (activeSession != null && activeSession.isCurrentlyActive &&
                            (activeSession.mode == FocusMode.STRICT || activeSession.mode == FocusMode.DEEP_FOCUS)
                        ) {
                            MinimalLog.w(
                                TAG,
                                "Focus session started while inside managed app $targetPackage. Intercepting."
                            )
                            recordBypassAttemptUseCase(
                                packageName = targetPackage,
                                route = BypassRoute.RECENTS,
                                userIntentReason = "Focus session started"
                            )
                            returnToHomeScreen()
                            stopMonitoring()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Error in foreground app monitoring ticker for $targetPackage", e)
            }
        }
    }

    private fun stopMonitoring() {
        activeMonitoringJob?.cancel()
        activeMonitoringJob = null
        currentForegroundPackage = null
    }

    private fun isSystemPackageWhitelisted(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower == "android" ||
                lower == "com.android.systemui" ||
                lower == "com.android.settings" ||
                lower.startsWith("com.google.android.inputmethod") ||
                lower.startsWith("com.android.inputmethod") ||
                lower.startsWith("com.samsung.android.honeyboard") ||
                lower.startsWith("com.swiftkey") ||
                lower.contains("inputmethod") ||
                lower.contains("keyboard") ||
                lower.contains("permissioncontroller")
    }

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

    private fun returnToHomeScreenWithExpiry(targetPackage: String, appLabel: String) {
        val homeIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = MainActivity.ACTION_TIME_LIMIT_EXPIRED
            putExtra(MainActivity.EXTRA_EXPIRED_PACKAGE, targetPackage)
            putExtra(MainActivity.EXTRA_EXPIRED_APP_LABEL, appLabel)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(homeIntent)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun returnToHomeScreen() {
        val homePerformed = performGlobalAction(GLOBAL_ACTION_HOME)
        if (!homePerformed) {
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
        stopMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
        MinimalLog.i(TAG, "Minimal Phone Bypass Protection Service Destroyed.")
    }
}


