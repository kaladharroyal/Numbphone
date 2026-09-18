package com.minimalphone.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LocalPhone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.model.AppBudget
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.DailyAppUsage
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.ui.theme.MutedText

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAppsManagement: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val essentialApps by viewModel.essentialApps.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val todayUsage by viewModel.todayUsage.collectAsStateWithLifecycle()
    val allInstalledApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importErrorText by remember { mutableStateOf<String?>(null) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    val feedbackFormUrl = "https://forms.gle/T3xsTRHTx2uDaoVP6"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Customization, Backup & Focus Control",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status Banner if available
            backupStatus?.let { status ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearBackupStatus() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Section 1: Minimalist Themes
                item {
                    SectionHeader(title = "MINIMALIST THEMES")
                }

                item {
                    ThemeSelectorCard(
                        currentTheme = settings.appTheme,
                        onSelectTheme = { viewModel.onSelectTheme(it) }
                    )
                }

                // Section 2: DND & Notification Control
                item {
                    SectionHeader(title = "NOTIFICATIONS & DND MODE")
                }

                item {
                    val isDndGranted = com.minimalphone.core.common.DndHelper.isNotificationPolicyAccessGranted(context)
                    DndControlCard(
                        isGranted = isDndGranted,
                        onGrantPermission = { com.minimalphone.core.common.DndHelper.openNotificationPolicySettings(context) }
                    )
                }

                // Section 3: Focus & Friction
                item {
                    SectionHeader(title = "FOCUS & FRICTION")
                }

                item {
                    SettingToggleCard(
                        title = "Adaptive Exit Friction",
                        description = "Progressive delays on repeated exits (5s -> 30s -> 2m -> 10m) to stop impulsive checks.",
                        isChecked = settings.isAdaptiveFrictionEnabled,
                        onCheckedChange = { viewModel.onToggleAdaptiveFriction(it) }
                    )
                }

                item {
                    DurationSettingCard(
                        currentDuration = settings.defaultFocusDurationMinutes,
                        onSelectDuration = { viewModel.onSelectDefaultDuration(it) }
                    )
                }

                // Section 3b: Dumb Phone Detox Mode
                item {
                    SectionHeader(title = "DUMB PHONE MODE (EXTREME DETOX)")
                }

                item {
                    SettingToggleCard(
                        title = "Dumb Phone Mode",
                        description = "Hides all managed apps and games from your home screen. Only essential communication tools (Phone, SMS) and whitelisted essentials stay visible.",
                        isChecked = settings.isDumbModeEnabled,
                        onCheckedChange = { viewModel.onToggleDumbMode(it) }
                    )
                }

                // Section 3c: Hardware Grayscale (M22)
                item {
                    SectionHeader(title = "HARDWARE GRAYSCALE & COLOR DETOX")
                }

                item {
                    GrayscaleControlCard(
                        isAutoGrayscaleInFocus = settings.isAutoGrayscaleInFocusEnabled,
                        onToggleAutoGrayscale = { viewModel.onToggleAutoGrayscaleInFocus(it) }
                    )
                }

                // Section 4: Applications & Whitelist
                item {
                    SectionHeader(title = "APPLICATIONS")
                }

                item {
                    NavigationActionCard(
                        title = "App Whitelist & Rules",
                        subtitle = "Classify apps as Essential or Managed with instant quick-search.",
                        onClick = onNavigateToAppsManagement
                    )
                }

                // Section 4b: Daily App Budgets
                item {
                    SectionHeader(title = "DAILY APP USAGE LIMITS")
                }

                item {
                    DailyAppBudgetsCard(
                        budgets = budgets,
                        todayUsage = todayUsage,
                        installedApps = allInstalledApps,
                        onAddBudgetClick = { showAddBudgetDialog = true },
                        onRemoveBudget = { pkg -> viewModel.onRemoveBudget(pkg) }
                    )
                }

                // Section 4c: Essential Access
                item {
                    SectionHeader(title = "EMERGENCY & ESSENTIAL ACCESS")
                }

                item {
                    EssentialAccessCard(
                        essentialApps = essentialApps,
                        onRemove = { pkg -> viewModel.onRemoveEssentialApp(pkg) }
                    )
                }

                // Section 5: Suggestions & Feedback
                item {
                    SectionHeader(title = "SUGGESTIONS & TIPS")
                }

                item {
                    SuggestionsAndTipsCard(
                        onOpenFeedback = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(feedbackFormUrl))
                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        }
                    )
                }

                // Section 6: Data Portability & Backup
                item {
                    SectionHeader(title = "LOCAL DATA BACKUP & RESTORE")
                }

                item {
                    BackupActionsCard(
                        onExport = {
                            viewModel.onExportBackup { json ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("MinimalPhone_Backup", json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_LONG).show()
                            }
                        },
                        onImport = {
                            importErrorText = null
                            importJsonText = ""
                            showImportDialog = true
                        }
                    )
                }

                // Section 7: System Health & Diagnostics
                item {
                    SectionHeader(title = "SYSTEM HEALTH")
                }

                item {
                    NavigationActionCard(
                        title = "Diagnostics & OEM Resilience",
                        subtitle = "Inspect live permissions, accessibility backstop, and battery exemptions.",
                        onClick = onNavigateToDiagnostics
                    )
                }

                // Section 8: Onboarding & Privacy
                item {
                    SectionHeader(title = "ONBOARDING & PRIVACY")
                }

                item {
                    NavigationActionCard(
                        title = "Setup Wizard",
                        subtitle = "Replay the initial introductory onboarding walkthrough.",
                        onClick = { viewModel.onReplayOnboarding(onNavigateToOnboarding) }
                    )
                }

                item {
                    PrivacyCharterCard()
                }
            }
        }
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Restore Local Backup",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste your previously exported Minimal Phone JSON configuration below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importErrorText = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("{\n  \"version\": 1,\n  ...", color = MutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    importErrorText?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF453A)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isBlank()) {
                            importErrorText = "JSON cannot be empty"
                            return@Button
                        }
                        viewModel.onImportBackup(importJsonText) { result ->
                            if (result.isSuccess) {
                                showImportDialog = false
                                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                importErrorText = result.errorMessage
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = MutedText)
                }
            }
        )
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            installedApps = allInstalledApps,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { pkg, label, limitMinutes, pinToHome ->
                viewModel.onSetBudget(pkg, label, limitMinutes, pinToHome)
                showAddBudgetDialog = false
            }
        )
    }

}

@Composable
private fun DndControlCard(
    isGranted: Boolean,
    onGrantPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Do Not Disturb (Calls Only)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isGranted) "DND Policy Active: notifications silenced during sessions, phone calls allowed." else "Permission required to automatically silence notifications during focus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) Color(0xFF32D74B) else MutedText
                )
            }
            if (!isGranted) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onGrantPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enable", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GrayscaleControlCard(
    isAutoGrayscaleInFocus: Boolean,
    onToggleAutoGrayscale: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isGrayscaleActive by remember {
        mutableStateOf(com.minimalphone.core.common.GrayscaleHelper.isGrayscaleEnabled(context))
    }
    val hasPermission = remember {
        com.minimalphone.core.common.GrayscaleHelper.hasSecureSettingsPermission(context)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Monochromacy (Grayscale)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Drains color from your entire phone display to make feeds and addictive apps visually unrewarding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isGrayscaleActive,
                onCheckedChange = { enable ->
                    if (hasPermission) {
                        val success = com.minimalphone.core.common.GrayscaleHelper.setGrayscaleEnabled(context, enable)
                        if (success) isGrayscaleActive = enable
                    } else {
                        com.minimalphone.core.common.GrayscaleHelper.openColorCorrectionSettings(context)
                        Toast.makeText(context, "Select 'Color correction' -> 'Grayscale'", Toast.LENGTH_LONG).show()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-Grayscale during Focus Sessions",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Automatically enables system monochrome when starting a focus session and restores color upon completion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isAutoGrayscaleInFocus,
                onCheckedChange = onToggleAutoGrayscale,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }

        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Automated Switching via ADB (Optional)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Run this one-time command on PC for instant automatic toggle:\n${com.minimalphone.core.common.GrayscaleHelper.ADB_PERMISSION_COMMAND}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MutedText
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionsAndTipsCard(
    onOpenFeedback: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Study & Focus Techniques",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "• Place phone screen face-down and out of arm's reach during deep study.\n• Use 25-minute Pomodoro intervals for maximum retention.\n• Keep only Phone and Messages in Always Available to eliminate dopamine loops.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            lineHeight = 20.sp
        )

        OutlinedButton(
            onClick = onOpenFeedback,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text("Send Feedback or Suggest a Feature", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MutedText,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun ThemeSelectorCard(
    currentTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        AppTheme.entries.forEach { theme ->
            val isSelected = theme == currentTheme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectTheme(theme) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupActionsCard(
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Local-First Configuration Portability",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Export or restore your app rules, whitelist categories, focus records, and preferences in JSON format without cloud dependencies.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export JSON", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }

            Button(
                onClick = onImport,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore JSON", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MutedText,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun DurationSettingCard(
    currentDuration: Int,
    onSelectDuration: (Int) -> Unit
) {
    val durationOptions = listOf(15, 25, 45, 60)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "Default Focus Session Length",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pre-selected target duration when starting a new session from home.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            durationOptions.forEach { minutes ->
                val isSelected = minutes == currentDuration
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectDuration(minutes) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${minutes}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    lineHeight = 18.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PrivacyCharterCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "100% On-Device Privacy Guarantee",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Minimal Phone operates entirely local-first. Your installed applications, focus goals, usage statistics, and block logs never leave your phone. No tracking SDKs, no cloud servers, no user accounts.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Version 1.2.0 • Phase 2",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )
    }
}

/**
 * Displays the list of essential apps that bypass ALL focus restrictions.
 * System defaults (Phone, Messages, Camera, etc.) are shown with a shield badge and cannot be removed.
 * User-added entries can be removed via the ✕ button.
 */
@Composable
private fun EssentialAccessCard(
    essentialApps: List<com.minimalphone.core.model.EssentialApp>,
    onRemove: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = androidx.compose.ui.Modifier.size(18.dp)
            )
            Text(
                text = "Essential Apps — Always Allowed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "These apps bypass all focus restrictions and budget rules unconditionally. Phone, Messages and other core services are set by default and cannot be removed.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (essentialApps.isEmpty()) {
            Text(
                text = "No essential apps configured yet. Launch the app to auto-detect defaults.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        } else {
            essentialApps.forEach { app ->
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalPhone,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = androidx.compose.ui.Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (app.isSystemDefault) {
                                Text(
                                    text = "System default",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (!app.isSystemDefault) {
                        TextButton(
                            onClick = { onRemove(app.packageName) }
                        ) {
                            Text(
                                text = "Remove",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF453A)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays user-defined daily app usage limits and current daily consumption.
 */
@Composable
private fun DailyAppBudgetsCard(
    budgets: List<AppBudget>,
    todayUsage: List<DailyAppUsage>,
    installedApps: List<InstalledApp> = emptyList(),
    onAddBudgetClick: () -> Unit,
    onRemoveBudget: (String) -> Unit
) {
    val usageMap = remember(todayUsage) { todayUsage.associateBy { it.packageName } }
    val appsMap = remember(installedApps) { installedApps.associateBy { it.packageName } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Daily App Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onAddBudgetClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add Budget",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = "Set hard daily limits. Once an app reaches its budget today, it is strictly blocked until midnight.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            lineHeight = 18.sp
        )

        if (budgets.isEmpty()) {
            Text(
                text = "No app budgets active. Tap + above to set a limit for distracting apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        } else {
            budgets.forEach { budget ->
                val used = usageMap[budget.packageName]?.foregroundMinutes ?: 0
                val progress = if (budget.dailyLimitMinutes > 0) {
                    (used.toFloat() / budget.dailyLimitMinutes).coerceIn(0f, 1f)
                } else 0f
                val isExceeded = used >= budget.dailyLimitMinutes
                val displayName = appsMap[budget.packageName]?.label ?: budget.appLabel.ifBlank { budget.packageName }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${used}m / ${budget.dailyLimitMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExceeded) Color(0xFFFF453A) else MutedText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onRemoveBudget(budget.packageName) },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    color = Color(0xFFFF453A),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = if (isExceeded) Color(0xFFFF453A) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Dialog to configure a daily time budget for a selected installed app.
 */
@Composable
private fun AddBudgetDialog(
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, appLabel: String, limitMinutes: Int, pinToHome: Boolean) -> Unit
) {
    var selectedPkg by remember { mutableStateOf(installedApps.firstOrNull()?.packageName ?: "") }
    var limitMinutes by remember { mutableStateOf(30) }
    var pinToHome by remember { mutableStateOf(true) }
    val presetDurations = listOf(15, 30, 45, 60, 90, 120)

    val selectedApp = installedApps.find { it.packageName == selectedPkg }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Set Daily App Limit",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select an app and set its maximum allowed daily usage:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )

                // App picker (simplified selection row)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    items(installedApps.size) { idx ->
                        val app = installedApps[idx]
                        val isSelected = app.packageName == selectedPkg
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPkg = app.packageName }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Daily Limit Duration:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetDurations.take(3).forEach { mins ->
                        val isSelected = limitMinutes == mins
                        OutlinedButton(
                            onClick = { limitMinutes = mins },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Text("${mins}m", fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MutedText)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetDurations.drop(3).forEach { mins ->
                        val isSelected = limitMinutes == mins
                        OutlinedButton(
                            onClick = { limitMinutes = mins },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Text("${mins}m", fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MutedText)
                        }
                    }
                }

                // Show on Home Screen Checkbox / Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pinToHome = !pinToHome }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show on Home Screen",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pin this app to your launcher home page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                    Switch(
                        checked = pinToHome,
                        onCheckedChange = { pinToHome = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPkg.isNotBlank()) {
                        onConfirm(selectedPkg, selectedApp?.label ?: selectedPkg, limitMinutes, pinToHome)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Set Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        }
    )
}


