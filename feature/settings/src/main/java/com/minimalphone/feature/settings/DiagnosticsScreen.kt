package com.minimalphone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.domain.boot.SystemDiagnosticsData
import com.minimalphone.core.ui.theme.DarkSurface
import com.minimalphone.core.ui.theme.MutedText
import com.minimalphone.core.ui.theme.PureBlack
import com.minimalphone.core.ui.theme.SuccessGreen

@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.diagnosticsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnostics()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header Row
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SYSTEM HEALTH",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Diagnostics & OEM Resilience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Diagnostic Check 1: Default Launcher
                item {
                    DiagnosticItemCard(
                        title = "Default Launcher",
                        description = "NumbPhone acts as your primary distraction-resistant home screen.",
                        isPassed = state.isDefaultLauncher,
                        actionLabel = if (state.isDefaultLauncher) "CONFIGURED" else "SET AS DEFAULT",
                        onActionClick = { viewModel.openDefaultLauncherSettings() }
                    )
                }

                // Diagnostic Check 2: Accessibility Backstop
                item {
                    DiagnosticItemCard(
                        title = "Accessibility Backstop",
                        description = "Required to intercept bypasses from notifications, recent apps, and deep links.",
                        isPassed = state.isAccessibilityEnabled,
                        actionLabel = if (state.isAccessibilityEnabled) "ENABLED" else "ENABLE SERVICE",
                        onActionClick = { viewModel.openAccessibilitySettings() }
                    )
                }

                // Diagnostic Check 3: Usage Access
                item {
                    DiagnosticItemCard(
                        title = "Usage Stats Access",
                        description = "Enables daily screen-time metrics and focus versus distraction analytics.",
                        isPassed = state.isUsageAccessGranted,
                        actionLabel = if (state.isUsageAccessGranted) "GRANTED" else "GRANT ACCESS",
                        onActionClick = { viewModel.openUsageSettings() }
                    )
                }

                // Diagnostic Check 4: Battery Optimization
                item {
                    DiagnosticItemCard(
                        title = "Battery Unrestricted",
                        description = "Prevents aggressive OEM task killers from terminating focus sessions and timers in the background.",
                        isPassed = state.isBatteryOptimizationIgnored,
                        actionLabel = if (state.isBatteryOptimizationIgnored) "UNRESTRICTED" else "EXEMPT BATTERY",
                        onActionClick = { viewModel.openBatterySettings() }
                    )
                }

                // OEM Specific Guidance Banner
                state.oemGuidance?.let { guidance ->
                    item {
                        OEMGuidanceCard(guidanceText = guidance)
                    }
                }

                // Active Session Telemetry
                item {
                    ActiveSessionCard(state = state)
                }

                // Hardware & Environment Info
                item {
                    DeviceInfoCard(state = state)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItemCard(
    title: String,
    description: String,
    isPassed: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isPassed) Color(0xFF27272A) else Color(0xFF52525B),
                RoundedCornerShape(12.dp)
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isPassed) SuccessGreen else Color(0xFFFF9F0A),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (isPassed) Color(0xFF1C1C1E) else Color.White,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable(enabled = !isPassed, onClick = onActionClick)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = actionLabel,
                        color = if (isPassed) MutedText else PureBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun OEMGuidanceCard(guidanceText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1605), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF5C3C00), RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "OEM BACKGROUND SURVIVAL",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFFB340),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = guidanceText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFEDEDED),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ActiveSessionCard(state: SystemDiagnosticsData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "FOCUS SESSION STATE",
                style = MaterialTheme.typography.labelLarge,
                color = MutedText,
                letterSpacing = 1.5.sp
            )
            val session = state.activeSession
            if (session != null) {
                Text(
                    text = "ACTIVE: ${session.goal.title}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Mode: ${session.mode} | Bypass Attempts: ${session.bypassAttemptsCount}",
                    color = MutedText,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    text = "No active focus session running.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp
                )
            }
            Text(
                text = "Today's Total Blocks: ${state.todayBlockedCount}",
                color = MutedText,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DeviceInfoCard(state: SystemDiagnosticsData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "DEVICE TELEMETRY",
                style = MaterialTheme.typography.labelLarge,
                color = MutedText,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "OEM: ${state.oemName.uppercase()} ${state.deviceModel}",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Text(
                text = "Android SDK Level: ${state.androidVersion}",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
    }
}
