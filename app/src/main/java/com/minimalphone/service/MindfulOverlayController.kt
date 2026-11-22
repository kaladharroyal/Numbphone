package com.minimalphone.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.minimalphone.MainActivity
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.model.PauseConfig
import com.minimalphone.core.ui.components.MindfulPauseScreen
import com.minimalphone.core.ui.theme.MinimalTheme

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

class MindfulOverlayController(
    private val service: AccessibilityService
) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = overlayView != null

    fun showOverlay(
        packageName: String,
        appLabel: String,
        config: PauseConfig,
        delaySeconds: Int,
        isReintervention: Boolean = false,
        onLeave: () -> Unit,
        onContinue: (reflectionReason: String?) -> Unit
    ) {
        if (overlayView != null) {
            hideOverlay()
        }

        try {
            val owner = OverlayLifecycleOwner()
            lifecycleOwner = owner

            val composeView = ComposeView(service).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    MinimalTheme {
                        MindfulPauseScreen(
                            packageName = packageName,
                            appLabel = appLabel,
                            config = config,
                            delaySeconds = delaySeconds,
                            isReintervention = isReintervention,
                            onLeave = {
                                hideOverlay()
                                onLeave()
                            },
                            onContinue = { reason ->
                                hideOverlay()
                                onContinue(reason)
                            }
                        )
                    }
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(composeView, params)
            overlayView = composeView
            MinimalLog.i("MindfulOverlayController", "Accessibility overlay attached successfully for $packageName")
        } catch (e: Exception) {
            MinimalLog.e("MindfulOverlayController", "Failed to attach accessibility overlay, falling back to Activity route", e)
            hideOverlay()
            fallbackToActivity(packageName, appLabel, delaySeconds)
        }
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (e: Exception) {
                MinimalLog.e("MindfulOverlayController", "Error removing overlay view", e)
            }
        }
        overlayView = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }

    private fun fallbackToActivity(packageName: String, appLabel: String, delaySeconds: Int) {
        try {
            val intent = Intent(service, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_PAUSE_PACKAGE", packageName)
                putExtra("EXTRA_PAUSE_LABEL", appLabel)
                putExtra("EXTRA_PAUSE_DELAY", delaySeconds)
            }
            service.startActivity(intent)
        } catch (e: Exception) {
            MinimalLog.e("MindfulOverlayController", "Error during fallback to MainActivity", e)
        }
    }
}
