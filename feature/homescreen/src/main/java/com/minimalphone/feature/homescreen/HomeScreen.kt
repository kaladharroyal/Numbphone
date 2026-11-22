package com.minimalphone.feature.homescreen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.model.GestureType
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.QuickContact
import com.minimalphone.core.ui.components.BlockedAppDialog
import com.minimalphone.core.ui.components.FocusStatusIndicator
import com.minimalphone.core.ui.components.IntentReflectionDialog
import com.minimalphone.core.ui.components.MindfulPauseScreen
import com.minimalphone.core.ui.components.MinimalAppItemRow
import com.minimalphone.core.ui.components.MinimalClockHeader
import com.minimalphone.core.ui.components.TimeLimitExpiredDialog
import com.minimalphone.core.ui.theme.MutedText
import kotlin.math.hypot

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToScreenTime: () -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pinnedContacts by viewModel.pinnedContacts.collectAsStateWithLifecycle()
    val unreadDigestCount by viewModel.unreadDigestCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var showControlPanel by remember { mutableStateOf(false) }

    val gestureRecognizer = remember(density) {
        GestureRecognizer(
            minDistancePx = with(density) { 96.dp.toPx() },
            minVelocityPxPerSec = with(density) { 400.dp.toPx() }
        )
    }

    var rootWidth by remember { mutableFloatStateOf(0f) }
    var rootHeight by remember { mutableFloatStateOf(0f) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPoint by remember { mutableStateOf(TouchPoint(0f, 0f, 0L)) }

    val isAnyOverlayOpen = showControlPanel ||
            uiState.isSearchOpen ||
            uiState.pauseScreen.isShowing ||
            uiState.blockedDialog.isShowing ||
            uiState.timeLimitExpiredDialog.isShowing ||
            uiState.reflectionDialog.isShowing

    val layoutDirection = LocalLayoutDirection.current
    val systemGestures = WindowInsets.systemGestures
    val leftGestureInset = systemGestures.getLeft(density, layoutDirection).toFloat()
    val rightGestureInset = systemGestures.getRight(density, layoutDirection).toFloat()
    val topGestureInset = systemGestures.getTop(density).toFloat()
    val bottomGestureInset = systemGestures.getBottom(density).toFloat()

    LaunchedEffect(Unit) {
        viewModel.pendingAutoLaunchApp.collect { targetResult ->
            if (targetResult != null) {
                viewModel.pendingAutoLaunchApp.value = null
                handleAppLaunch(context, targetResult.installedApp)
            }
        }
    }

    // Immersive Full-Screen Mode: Hide status bar on Home Screen
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .onSizeChanged {
                rootWidth = it.width.toFloat()
                rootHeight = it.height.toFloat()
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Search") {
                        viewModel.openSearch()
                        true
                    },
                    CustomAccessibilityAction("Control Center") {
                        showControlPanel = true
                        true
                    },
                    CustomAccessibilityAction("Settings") {
                        onNavigateToSettings()
                        true
                    },
                    CustomAccessibilityAction("Focus Mode") {
                        onNavigateToFocus()
                        true
                    },
                    CustomAccessibilityAction("Lock Screen") {
                        viewModel.onGestureRecognized(
                            type = GestureType.DOUBLE_TAP,
                            onNavigateToControlCenter = { showControlPanel = true },
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToFocus = onNavigateToFocus,
                            onAllowLaunch = { pkg, _ ->
                                val app = uiState.visibleApps.find { it.packageName == pkg }
                                    ?: InstalledApp(packageName = pkg, activityName = "", label = pkg)
                                handleAppLaunch(context, app)
                            },
                            onShowMessage = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                        true
                    }
                )
            }
            .pointerInput(isAnyOverlayOpen, rootWidth, rootHeight, leftGestureInset, rightGestureInset, topGestureInset, bottomGestureInset) {
                if (isAnyOverlayOpen) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    val startPoint = TouchPoint(down.position.x, down.position.y, startTime)
                    var lastPoint = startPoint
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            lastPoint = TouchPoint(change.position.x, change.position.y, System.currentTimeMillis())
                            val dx = lastPoint.x - startPoint.x
                            val dy = lastPoint.y - startPoint.y
                            if (hypot(dx.toDouble(), dy.toDouble()) > 20) {
                                isDrag = true
                            }
                        } else {
                            val endTime = System.currentTimeMillis()
                            if (isDrag) {
                                val bounds = ScreenBounds(rootWidth, rootHeight, leftGestureInset, rightGestureInset, topGestureInset, bottomGestureInset)
                                val classified = gestureRecognizer.classifySwipe(startPoint, lastPoint, bounds)
                                if (classified != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onGestureRecognized(
                                        type = classified,
                                        onNavigateToControlCenter = { showControlPanel = true },
                                        onNavigateToSettings = onNavigateToSettings,
                                        onNavigateToFocus = onNavigateToFocus,
                                        onAllowLaunch = { pkg, _ ->
                                            val app = uiState.visibleApps.find { it.packageName == pkg }
                                                ?: InstalledApp(packageName = pkg, activityName = "", label = pkg)
                                            handleAppLaunch(context, app)
                                        },
                                        onShowMessage = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            } else {
                                if (endTime - lastTapTime < 300L && hypot((startPoint.x - lastTapPoint.x).toDouble(), (startPoint.y - lastTapPoint.y).toDouble()) < 50) {
                                    lastTapTime = 0L
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onGestureRecognized(
                                        type = GestureType.DOUBLE_TAP,
                                        onNavigateToControlCenter = { showControlPanel = true },
                                        onNavigateToSettings = onNavigateToSettings,
                                        onNavigateToFocus = onNavigateToFocus,
                                        onAllowLaunch = { pkg, _ ->
                                            val app = uiState.visibleApps.find { it.packageName == pkg }
                                                ?: InstalledApp(packageName = pkg, activityName = "", label = pkg)
                                            handleAppLaunch(context, app)
                                        },
                                        onShowMessage = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    lastTapTime = endTime
                                    lastTapPoint = startPoint
                                }
                            }
                            break
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Screen Time (left), Control Center (center), Settings (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToScreenTime) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = "Screen Time",
                        tint = MutedText
                    )
                }

                // Center: Quick Control Center & Notification Shield Button with unread badge
                Box {
                    IconButton(onClick = { showControlPanel = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Control Center & Notifications",
                            tint = MutedText
                        )
                    }
                    if (unreadDigestCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MutedText
                    )
                }
            }

            // Central Area: Clock, Quick Contacts, & Allowed Apps List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showControlPanel = true },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onGestureRecognized(
                                type = GestureType.LONG_PRESS_CLOCK,
                                onNavigateToControlCenter = { showControlPanel = true },
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToFocus = onNavigateToFocus,
                                onAllowLaunch = { pkg, _ ->
                                    val app = uiState.visibleApps.find { it.packageName == pkg }
                                        ?: InstalledApp(packageName = pkg, activityName = "", label = pkg)
                                    handleAppLaunch(context, app)
                                },
                                onShowMessage = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                ) {
                    MinimalClockHeader(
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                // Visible Search Text Affordance Button (Accessibility & fallback)
                TextButton(
                    onClick = { viewModel.openSearch() },
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search apps and contacts",
                        tint = MutedText,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text(
                        text = "Search",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // One-time hint for gestures (M23.4)
                if (!uiState.gestureHintShown) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.dismissGestureHint() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tip: Swipe up to search",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Quick Pinned Contacts Row (M20)
                if (pinnedContacts.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pinnedContacts, key = { it.id }) { contact ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { dialContact(context, contact.phoneNumber) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Call,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    items(
                        items = uiState.visibleApps,
                        key = { it.packageName }
                    ) { app ->
                        MinimalAppItemRow(
                            label = app.label,
                            onClick = {
                                viewModel.onAppClicked(app) { pkg, _ ->
                                    handleAppLaunch(context, app)
                                }
                            }
                        )
                    }

                    // + Add App button (hidden in Dumb Mode for total detox)
                    if (!uiState.isDumbMode) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onNavigateToApps,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MutedText,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = "Add app",
                                    color = MutedText,
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Bottom Area: SOS button + Focus Mode status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isDumbMode && !uiState.isFocusActive) {
                    Text(
                        text = "DUMB PHONE MODE • ESSENTIALS ONLY",
                        color = MutedText.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                FocusStatusIndicator(
                    isFocusActive = uiState.isFocusActive,
                    focusGoal = uiState.focusGoal,
                    remainingMinutes = uiState.remainingMinutes,
                    onClick = onNavigateToFocus,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // SOS Emergency Call button — always visible, always launches dialer
                TextButton(
                    onClick = { AppLauncher.openDialer(context) },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Emergency Call",
                        tint = MutedText.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "Emergency Call",
                        color = MutedText.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Blocked Feedback Dialog
        if (uiState.blockedDialog.isShowing) {
            BlockedAppDialog(
                packageName = uiState.blockedDialog.packageName,
                session = uiState.blockedDialog.activeSession,
                reason = uiState.blockedDialog.reason,
                onDismiss = { viewModel.dismissBlockedDialog() }
            )
        }

        // Daily Time Limit Expired Dialog (10-Second Countdown & Emergency Extension)
        if (uiState.timeLimitExpiredDialog.isShowing) {
            TimeLimitExpiredDialog(
                appLabel = uiState.timeLimitExpiredDialog.appLabel,
                packageName = uiState.timeLimitExpiredDialog.packageName,
                onCloseApp = { viewModel.dismissTimeLimitExpiredDialog() },
                onExtendEmergencyTime = { additionalMinutes ->
                    viewModel.extendEmergencyTime(
                        packageName = uiState.timeLimitExpiredDialog.packageName,
                        additionalMinutes = additionalMinutes
                    )
                }
            )
        }

        // Intent Reflection Dialog ("Why are you opening this?")
        if (uiState.reflectionDialog.isShowing) {
            IntentReflectionDialog(
                appLabel = uiState.reflectionDialog.appLabel,
                onDismiss = { viewModel.dismissReflectionDialog() },
                onSubmitReflection = { reason ->
                    viewModel.onRecordReflection(reason) { pkg, act ->
                        val targetApp = uiState.visibleApps.find { it.packageName == pkg }
                            ?: com.minimalphone.core.model.InstalledApp(
                                packageName = pkg,
                                activityName = act,
                                label = uiState.reflectionDialog.appLabel
                            )
                        handleAppLaunch(context, targetApp)
                    }
                }
            )
        }

        // Minimalist Control Center & Notification Shield Sheet
        if (showControlPanel) {
            MinimalControlPanelSheet(
                onDismiss = { showControlPanel = false },
                onNavigateToFocus = onNavigateToFocus,
                onNavigateToScreenTime = onNavigateToScreenTime,
                onNavigateToSettings = onNavigateToSettings
            )
        }

        // M22: Mindful Pause Full-Screen Overlay (Launcher Path)
        if (uiState.pauseScreen.isShowing) {
            val pauseState = uiState.pauseScreen
            MindfulPauseScreen(
                packageName = pauseState.packageName,
                appLabel = pauseState.appLabel,
                config = pauseState.config,
                delaySeconds = pauseState.delaySeconds,
                isReintervention = false,
                onLeave = {
                    viewModel.onPauseLeave(pauseState.packageName, pauseState.delaySeconds)
                },
                onContinue = { reflectionReason ->
                    viewModel.onPauseContinue(
                        packageName = pauseState.packageName,
                        activityName = pauseState.activityName,
                        config = pauseState.config,
                        delaySeconds = pauseState.delaySeconds,
                        reflectionReason = reflectionReason
                    ) { pkg, _ ->
                        AppLauncher.launchAppByPackage(context, pkg)
                    }
                }
            )
        }
        // M23: Home Search Overlay
        if (uiState.isSearchOpen) {
            HomeSearchOverlay(
                query = uiState.searchQuery,
                results = uiState.searchResults,
                autoLaunchCountdownApp = uiState.autoLaunchCountdownApp,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onCancelAutoLaunch = { viewModel.cancelAutoLaunch() },
                onLaunchResult = { result ->
                    viewModel.onLaunchSearchResult(
                        result = result,
                        onAllowLaunch = { pkg, _ ->
                            val app = uiState.visibleApps.find { it.packageName == pkg }
                                ?: InstalledApp(packageName = pkg, activityName = "", label = pkg)
                            handleAppLaunch(context, app)
                        },
                        onDialNumber = { number ->
                            dialContact(context, number)
                        }
                    )
                },
                onToggleFavorite = { pkg, isFav ->
                    viewModel.toggleFavoriteOnHome(pkg, isFav)
                },
                onSaveTimeLimit = { pkg, limit ->
                    viewModel.saveTimeLimit(pkg, limit)
                },
                onRemoveTimeLimit = { pkg ->
                    viewModel.removeTimeLimit(pkg)
                },
                onSavePauseConfig = { config ->
                    viewModel.savePauseConfig(config)
                },
                getTimeLimitForPackage = { pkg ->
                    uiState.limitsMap[pkg]
                },
                getPauseConfigForPackage = { pkg ->
                    uiState.pauseConfigsMap[pkg]
                },
                onDismiss = { viewModel.closeSearch() }
            )
        }
    }
}

private fun dialContact(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        AppLauncher.openDialer(context)
    }
}

private fun handleAppLaunch(context: Context, app: InstalledApp) {
    when (app.label.lowercase()) {
        "phone" -> AppLauncher.openDialer(context)
        "messages" -> AppLauncher.openMessages(context)
        "settings" -> {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                AppLauncher.launchAppByPackage(context, app.packageName)
            }
        }
        else -> {
            AppLauncher.launchAppByPackage(context, app.packageName)
        }
    }
}
