package com.minimalphone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.minimalphone.core.domain.timelimit.TimeLimitExpiryNotifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var timeLimitExpiryNotifier: TimeLimitExpiryNotifier

    companion object {
        const val ACTION_TIME_LIMIT_EXPIRED = "com.minimalphone.action.TIME_LIMIT_EXPIRED"
        const val EXTRA_EXPIRED_PACKAGE = "extra_expired_package"
        const val EXTRA_EXPIRED_APP_LABEL = "extra_expired_app_label"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        handleExpiredIntent(intent)

        setContent {
            MinimalPhoneNavHost()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExpiredIntent(intent)
    }

    private fun handleExpiredIntent(intent: Intent?) {
        if (intent == null) return
        val expiredPackage = intent.getStringExtra(EXTRA_EXPIRED_PACKAGE)
        if (!expiredPackage.isNullOrBlank()) {
            val appLabel = intent.getStringExtra(EXTRA_EXPIRED_APP_LABEL) ?: expiredPackage.substringAfterLast('.')
            timeLimitExpiryNotifier.notifyTimeLimitExpired(expiredPackage, appLabel)
        }
    }
}

