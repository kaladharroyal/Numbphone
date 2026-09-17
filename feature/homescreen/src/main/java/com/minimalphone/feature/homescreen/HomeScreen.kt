package com.minimalphone.feature.homescreen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.ui.components.BlockedAppDialog
import com.minimalphone.core.ui.components.FocusStatusIndicator
import com.minimalphone.core.ui.components.IntentReflectionDialog
import com.minimalphone.core.ui.components.MinimalAppItemRow
import com.minimalphone.core.ui.components.MinimalClockHeader
import com.minimalphone.core.ui.theme.MutedText
import com.minimalphone.core.ui.theme.PureBlack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToScreenTime: () -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showControlPanel by remember { mutableStateOf(false) }

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

                // Center: Quick Control Center & Notification Shield Button
                IconButton(onClick = { showControlPanel = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Control Center & Notifications",
                        tint = MutedText
                    )
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MutedText
                    )
                }
            }

            // Central Area: Clock & Allowed Apps List
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
                        modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
                    )
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

                    // + Add App button at the bottom of the list
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

            // Bottom Area: Focus Mode status
            FocusStatusIndicator(
                isFocusActive = uiState.isFocusActive,
                focusGoal = uiState.focusGoal,
                remainingMinutes = uiState.remainingMinutes,
                onClick = onNavigateToFocus,
                modifier = Modifier.padding(bottom = 24.dp)
            )
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

        // Intent Reflection Dialog ("Why are you opening this?")
        if (uiState.reflectionDialog.isShowing) {
            IntentReflectionDialog(
                appLabel = uiState.reflectionDialog.appLabel,
                onDismiss = { viewModel.dismissReflectionDialog() },
                onSubmitReflection = { reason ->
                    viewModel.onRecordReflection(reason)
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

private fun handleAppLaunch(context: android.content.Context, app: InstalledApp) {
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
