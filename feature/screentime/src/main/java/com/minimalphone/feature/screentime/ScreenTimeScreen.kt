package com.minimalphone.feature.screentime

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.AppUsageStat
import com.minimalphone.core.model.BehavioralInsights
import com.minimalphone.core.model.WeeklyWellbeingReport
import com.minimalphone.core.ui.components.SetTimeLimitDialog
import com.minimalphone.core.ui.theme.DarkSurface
import com.minimalphone.core.ui.theme.MutedText
import com.minimalphone.core.ui.theme.PureBlack

@Composable
fun ScreenTimeScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScreenTimeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedAppStat by remember { mutableStateOf<AppUsageStat?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Screen Time & Wellbeing",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = { viewModel.loadStats() }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh stats",
                        tint = MutedText
                    )
                }
            }

            if (!uiState.summary.hasUsagePermission) {
                PermissionRequiredCard(
                    onGrantClick = {
                        openUsageAccessSettings(context)
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // 1. Today's Screen Time Hero
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "TODAY'S SCREEN TIME",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.summary.formattedTotalTime,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Light
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress bar showing managed vs essential
                            LinearProgressIndicator(
                                progress = { uiState.summary.managedPercentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFFFF453A),
                                trackColor = Color(0xFF32D74B),
                                strokeCap = StrokeCap.Round
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Managed: ${(uiState.summary.managedTimeMillis / 60000)}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF453A)
                                )
                                Text(
                                    text = "Essential: ${(uiState.summary.essentialTimeMillis / 60000)}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF32D74B)
                                )
                            }
                        }
                    }

                    // 2. Behavioral Insights Metrics (M18)
                    item {
                        BehavioralInsightsSection(
                            insights = uiState.insights,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    // 3. Weekly Wellbeing Report Card (M19)
                    item {
                        WeeklyWellbeingCard(
                            report = uiState.weeklyReport,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    // 4. Per-App Usage Breakdown Header
                    item {
                        Text(
                            text = "APP USAGE BREAKDOWN",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = MutedText,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    // App Usage List items
                    items(
                        items = uiState.summary.appStats,
                        key = { it.packageName }
                    ) { appStat ->
                        AppUsageRow(
                            appStat = appStat,
                            timeLimit = uiState.limitsMap[appStat.packageName],
                            onSetLimitClick = { selectedAppStat = appStat }
                        )
                    }
                }
            }
        }

        // Daily Time Limit Dialog
        if (selectedAppStat != null) {
            val stat = selectedAppStat!!
            val currentLimit = uiState.limitsMap[stat.packageName]?.dailyLimitMinutes

            SetTimeLimitDialog(
                appLabel = stat.label,
                currentLimitMinutes = currentLimit,
                todayUsedMinutes = stat.totalMinutes,
                onDismiss = { selectedAppStat = null },
                onSaveLimit = { limitMinutes ->
                    viewModel.setAppTimeLimit(stat.packageName, limitMinutes)
                },
                onRemoveLimit = {
                    viewModel.removeAppTimeLimit(stat.packageName)
                }
            )
        }
    }
}

@Composable
private fun BehavioralInsightsSection(
    insights: BehavioralInsights,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InsightStatBox(
            title = "Distractions Blocked",
            value = "${insights.totalBlockedAttemptsToday}",
            icon = Icons.Outlined.Block,
            modifier = Modifier.weight(1f)
        )
        InsightStatBox(
            title = "Deep Focus Time",
            value = "${insights.totalFocusMinutesToday}m",
            icon = Icons.Outlined.SelfImprovement,
            modifier = Modifier.weight(1f)
        )
        InsightStatBox(
            title = "Reflections Logged",
            value = "${insights.totalReflectionsCountToday}",
            icon = Icons.Outlined.Psychology,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InsightStatBox(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun WeeklyWellbeingCard(
    report: WeeklyWellbeingReport,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Weekly Wellbeing Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${report.digitalBalanceScore}/100 Balance",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = report.keyTakeaway,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weekly Screen Time: ${report.totalScreenTimeHours}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
                Text(
                    text = "Focus Hours: ${report.totalFocusHours}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF32D74B)
                )
            }
        }
    }
}

@Composable
fun AppUsageRow(
    appStat: AppUsageStat,
    timeLimit: AppTimeLimit? = null,
    onSetLimitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLimitExceeded = timeLimit != null && timeLimit.isEnabled && appStat.totalMinutes >= timeLimit.dailyLimitMinutes

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSetLimitClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appStat.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            val statusSubtitle = if (appStat.isEssential) "Essential" else "Managed"
            val limitSubtitle = when {
                isLimitExceeded -> " • 🚫 Limit Reached (${timeLimit!!.formattedLimit})"
                timeLimit != null -> " • ⏳ Limit: ${timeLimit.formattedLimit}"
                else -> ""
            }

            Text(
                text = statusSubtitle + limitSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = if (isLimitExceeded) Color(0xFFFF453A) else if (timeLimit != null) Color(0xFFFF9F0A) else if (appStat.isEssential) Color(0xFF32D74B) else MutedText
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = appStat.formattedDuration,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (isLimitExceeded) Color(0xFFFF453A) else MaterialTheme.colorScheme.onBackground
            )

            IconButton(onClick = onSetLimitClick) {
                Icon(
                    imageVector = Icons.Outlined.HourglassBottom,
                    contentDescription = "Set Limit",
                    tint = if (isLimitExceeded) Color(0xFFFF453A) else if (timeLimit != null) Color(0xFFFF9F0A) else MutedText
                )
            }
        }
    }
}

@Composable
fun PermissionRequiredCard(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Usage Access Required",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Minimal Phone requires Usage Access permission to track screen time on your device. Your data remains strictly local and is never uploaded anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Grant Usage Access")
            }
        }
    }
}

private fun openUsageAccessSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
