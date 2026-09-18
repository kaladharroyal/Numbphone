package com.minimalphone.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.NotificationDigestRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MinimalNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var focusSessionRepository: FocusSessionRepository

    @Inject
    lateinit var essentialAppRepository: EssentialAppRepository

    @Inject
    lateinit var digestRepository: NotificationDigestRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "MinimalNotificationListener"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.isOngoing) return

        val pkgName = sbn.packageName ?: return
        if (pkgName == packageName) return // Ignore own app

        serviceScope.launch {
            try {
                val activeSession = focusSessionRepository.getActiveSessionSync()
                val isFocusActive = activeSession != null && activeSession.isCurrentlyActive

                val essentialPkgs = essentialAppRepository.getEssentialPackages()
                val isEssential = essentialPkgs.contains(pkgName) ||
                        pkgName.contains("dialer", ignoreCase = true) ||
                        pkgName.contains("telecom", ignoreCase = true) ||
                        pkgName.contains("phone", ignoreCase = true)

                // If focus is active and app is not essential, intercept and bundle into digest
                if (isFocusActive && !isEssential) {
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

                    val appLabel = runCatching {
                        val pm = applicationContext.packageManager
                        val appInfo = pm.getApplicationInfo(pkgName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    }.getOrDefault(pkgName)

                    digestRepository.recordNotification(
                        packageName = pkgName,
                        appLabel = appLabel,
                        title = title,
                        text = text
                    )

                    // Cancel the visual distraction from the status bar
                    cancelNotification(sbn.key)
                    MinimalLog.i(TAG, "Bundled distracting notification from $appLabel into Focus Digest.")
                }
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Error handling notification", e)
            }
        }
    }
}
