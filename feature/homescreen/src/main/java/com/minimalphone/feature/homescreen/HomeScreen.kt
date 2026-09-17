package com.minimalphone.feature.homescreen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToScreenTime: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
            // Header: Screen Time button (left) and Settings button (right)
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
                MinimalClockHeader(
                    modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
                )

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
