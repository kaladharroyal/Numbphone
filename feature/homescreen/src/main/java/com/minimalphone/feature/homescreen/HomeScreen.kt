package com.minimalphone.feature.homescreen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.QuickContact
import com.minimalphone.core.ui.components.BlockedAppDialog
import com.minimalphone.core.ui.components.FocusStatusIndicator
import com.minimalphone.core.ui.components.IntentReflectionDialog
import com.minimalphone.core.ui.components.MinimalAppItemRow
import com.minimalphone.core.ui.components.MinimalClockHeader
import com.minimalphone.core.ui.components.TimeLimitExpiredDialog
import com.minimalphone.core.ui.theme.MutedText

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
    var showControlPanel by remember { mutableStateOf(false) }

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
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showControlPanel = true }
                    )
                ) {
                    MinimalClockHeader(
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
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
