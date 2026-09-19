package com.minimalphone.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalPhone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.EssentialApp
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.ui.theme.MutedText

enum class SettingsSection(val title: String, val subtitle: String) {
    MAIN("SETTINGS", "Customization, Backup & Focus Control"),
    APPEARANCE("APPEARANCE & THEMES", "Minimalist color schemes and visual style"),
    FOCUS_DETOX("FOCUS & DETOX", "Adaptive friction, durations & dumb phone mode"),
    APP_LIMITS("APP LIMITS & WHITELIST", "Usage budgets, restrictions & emergency access"),
    SYSTEM_PERMISSIONS("SYSTEM & DIAGNOSTICS", "DND access, live health & setup wizard"),
    DATA_ABOUT("DATA & ABOUT", "Local JSON backups, tips & privacy charter")
}

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
    val timeLimits by viewModel.timeLimits.collectAsStateWithLifecycle()
    val allInstalledApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var currentSection by remember { mutableStateOf(SettingsSection.MAIN) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importErrorText by remember { mutableStateOf<String?>(null) }
    var showAddTimeLimitDialog by remember { mutableStateOf(false) }

    val feedbackFormUrl = "https://forms.gle/T3xsTRHTx2uDaoVP6"
    val isDndGranted = com.minimalphone.core.common.DndHelper.isNotificationPolicyAccessGranted(context)

    BackHandler(enabled = currentSection != SettingsSection.MAIN) {
        currentSection = SettingsSection.MAIN
    }

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
                IconButton(onClick = {
                    if (currentSection == SettingsSection.MAIN) {
                        onNavigateBack()
                    } else {
                        currentSection = SettingsSection.MAIN
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = currentSection.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = currentSection.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toast/Status Banner if active
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

            AnimatedContent(
                targetState = currentSection,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SettingsSectionTransition"
            ) { section ->
                when (section) {
                    SettingsSection.MAIN -> {
                        SettingsHubView(
                            isDndGranted = isDndGranted,
                            onGrantDnd = { com.minimalphone.core.common.DndHelper.openNotificationPolicySettings(context) },
                            onSelectSection = { currentSection = it },
                            onNavigateToAppsManagement = onNavigateToAppsManagement,
                            onNavigateToDiagnostics = onNavigateToDiagnostics
                        )
                    }
                    SettingsSection.APPEARANCE -> {
                        AppearanceSettingsView(
                            currentTheme = settings.appTheme,
                            onSelectTheme = { viewModel.onSelectTheme(it) }
                        )
                    }
                    SettingsSection.FOCUS_DETOX -> {
                        FocusDetoxSettingsView(
                            settings = settings,
                            onToggleAdaptiveFriction = { viewModel.onToggleAdaptiveFriction(it) },
                            onSelectDefaultDuration = { viewModel.onSelectDefaultDuration(it) },
                            onToggleDumbMode = { viewModel.onToggleDumbMode(it) },
                            onToggleAutoGrayscale = { viewModel.onToggleAutoGrayscaleInFocus(it) }
                        )
                    }
                    SettingsSection.APP_LIMITS -> {
                        AppLimitsSettingsView(
                            timeLimits = timeLimits,
                            essentialApps = essentialApps,
                            installedApps = allInstalledApps,
                            onNavigateToAppsManagement = onNavigateToAppsManagement,
                            onAddLimitClick = { showAddTimeLimitDialog = true },
                            onRemoveLimit = { pkg -> viewModel.onRemoveTimeLimit(pkg) },
                            onRemoveEssential = { pkg -> viewModel.onRemoveEssentialApp(pkg) }
                        )
                    }
                    SettingsSection.SYSTEM_PERMISSIONS -> {
                        SystemPermissionsSettingsView(
                            isDndGranted = isDndGranted,
                            onGrantDnd = { com.minimalphone.core.common.DndHelper.openNotificationPolicySettings(context) },
                            onNavigateToDiagnostics = onNavigateToDiagnostics,
                            onReplayOnboarding = { viewModel.onReplayOnboarding(onNavigateToOnboarding) }
                        )
                    }
                    SettingsSection.DATA_ABOUT -> {
                        DataAboutSettingsView(
                            onExportBackup = {
                                viewModel.onExportBackup { json ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("MinimalPhone_Backup", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_LONG).show()
                                }
                            },
                            onImportBackup = {
                                importErrorText = null
                                importJsonText = ""
                                showImportDialog = true
                            },
                            onOpenFeedback = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(feedbackFormUrl))
                                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                context.startActivity(intent)
                            }
                        )
                    }
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
                        text = "Paste your previously exported NumbPhone JSON configuration below:",
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

    if (showAddTimeLimitDialog) {
        AddTimeLimitDialog(
            installedApps = allInstalledApps,
            onDismiss = { showAddTimeLimitDialog = false },
            onConfirm = { pkg, limitMinutes, pinToHome ->
                viewModel.onSetTimeLimit(pkg, limitMinutes, pinToHome)
                showAddTimeLimitDialog = false
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 1. Settings Hub View (Top Level)
// -------------------------------------------------------------------------------------------------
@Composable
private fun SettingsHubView(
    isDndGranted: Boolean,
    onGrantDnd: () -> Unit,
    onSelectSection: (SettingsSection) -> Unit,
    onNavigateToAppsManagement: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Quick Permission Alert Banner (only if DND not granted)
        if (!isDndGranted) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "DND Access Recommended",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Allow silencing notifications during focus sessions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onGrantDnd,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            CategoryCard(
                icon = Icons.Outlined.Palette,
                title = "Appearance & Themes",
                subtitle = "Dark, Light, OLED Black & E-Ink Monochrome modes",
                onClick = { onSelectSection(SettingsSection.APPEARANCE) }
            )
        }

        item {
            CategoryCard(
                icon = Icons.Outlined.Timer,
                title = "Focus & Detox Controls",
                subtitle = "Adaptive exit friction, session duration, dumb phone mode & grayscale",
                onClick = { onSelectSection(SettingsSection.FOCUS_DETOX) }
            )
        }

        item {
            CategoryCard(
                icon = Icons.Outlined.HourglassTop,
                title = "App Limits & Whitelist",
                subtitle = "App classification, daily usage limits & essential emergency apps",
                onClick = { onSelectSection(SettingsSection.APP_LIMITS) }
            )
        }

        item {
            CategoryCard(
                icon = Icons.Outlined.Shield,
                title = "System & Diagnostics",
                subtitle = "Live permissions health, OEM resilience & setup wizard",
                onClick = { onSelectSection(SettingsSection.SYSTEM_PERMISSIONS) }
            )
        }

        item {
            CategoryCard(
                icon = Icons.Outlined.Info,
                title = "Data & About",
                subtitle = "Local JSON backup/restore, focus tips & privacy charter",
                onClick = { onSelectSection(SettingsSection.DATA_ABOUT) }
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. Appearance & Themes View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AppearanceSettingsView(
    currentTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SectionHeader(title = "MINIMALIST THEMES")
        }

        item {
            ThemeSelectorCard(
                currentTheme = currentTheme,
                onSelectTheme = onSelectTheme
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. Focus & Detox Settings View
// -------------------------------------------------------------------------------------------------
@Composable
private fun FocusDetoxSettingsView(
    settings: com.minimalphone.core.domain.UserSettings,
    onToggleAdaptiveFriction: (Boolean) -> Unit,
    onSelectDefaultDuration: (Int) -> Unit,
    onToggleDumbMode: (Boolean) -> Unit,
    onToggleAutoGrayscale: (Boolean) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SectionHeader(title = "FRICTION & DURATION")
        }

        item {
            SettingToggleCard(
                title = "Adaptive Exit Friction",
                description = "Progressive delays on repeated exits (5s -> 30s -> 2m -> 10m) to stop impulsive checks.",
                isChecked = settings.isAdaptiveFrictionEnabled,
                onCheckedChange = onToggleAdaptiveFriction
            )
        }

        item {
            DurationSettingCard(
                currentDuration = settings.defaultFocusDurationMinutes,
                onSelectDuration = onSelectDefaultDuration
            )
        }

        item {
            SectionHeader(title = "EXTREME DETOX")
        }

        item {
            SettingToggleCard(
                title = "Dumb Phone Mode",
                description = "Hides all managed apps and games from your home screen. Only essential communication tools (Phone, SMS) and whitelisted essentials stay visible.",
                isChecked = settings.isDumbModeEnabled,
                onCheckedChange = onToggleDumbMode
            )
        }

        item {
            SectionHeader(title = "HARDWARE MONOCHROMACY")
        }

        item {
            GrayscaleControlCard(
                isAutoGrayscaleInFocus = settings.isAutoGrayscaleInFocusEnabled,
                onToggleAutoGrayscale = onToggleAutoGrayscale
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. App Limits & Whitelist View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AppLimitsSettingsView(
    timeLimits: List<AppTimeLimit>,
    essentialApps: List<EssentialApp>,
    installedApps: List<InstalledApp>,
    onNavigateToAppsManagement: () -> Unit,
    onAddLimitClick: () -> Unit,
    onRemoveLimit: (String) -> Unit,
    onRemoveEssential: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SectionHeader(title = "CLASSIFICATION & RULES")
        }

        item {
            NavigationActionCard(
                title = "App Whitelist & Rules",
                subtitle = "Classify apps as Essential or Managed with instant quick-search.",
                onClick = onNavigateToAppsManagement
            )
        }

        item {
            SectionHeader(title = "DAILY USAGE BUDGETS")
        }

        item {
            DailyAppLimitsCard(
                limits = timeLimits,
                installedApps = installedApps,
                onAddLimitClick = onAddLimitClick,
                onRemoveLimit = onRemoveLimit
            )
        }

        item {
            SectionHeader(title = "EMERGENCY & ESSENTIAL ACCESS")
        }

        item {
            EssentialAccessCard(
                essentialApps = essentialApps,
                onRemove = onRemoveEssential
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 5. System & Permissions View
// -------------------------------------------------------------------------------------------------
@Composable
private fun SystemPermissionsSettingsView(
    isDndGranted: Boolean,
    onGrantDnd: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onReplayOnboarding: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SectionHeader(title = "HEALTH & RESILIENCE")
        }

        item {
            NavigationActionCard(
                title = "Diagnostics & OEM Resilience",
                subtitle = "Inspect live permissions, accessibility backstop, and battery exemptions.",
                onClick = onNavigateToDiagnostics
            )
        }

        item {
            SectionHeader(title = "NOTIFICATION POLICY")
        }

        item {
            DndControlCard(
                isGranted = isDndGranted,
                onGrantPermission = onGrantDnd
            )
        }

        item {
            SectionHeader(title = "SETUP & ONBOARDING")
        }

        item {
            NavigationActionCard(
                title = "Setup Wizard",
                subtitle = "Replay the initial introductory onboarding walkthrough.",
                onClick = onReplayOnboarding
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 6. Data, Backup & About View
// -------------------------------------------------------------------------------------------------
@Composable
private fun DataAboutSettingsView(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenFeedback: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SectionHeader(title = "LOCAL BACKUP & RESTORE")
        }

        item {
            BackupActionsCard(
                onExport = onExportBackup,
                onImport = onImportBackup
            )
        }

        item {
            SectionHeader(title = "SUGGESTIONS & TIPS")
        }

        item {
            SuggestionsAndTipsCard(onOpenFeedback = onOpenFeedback)
        }

        item {
            SectionHeader(title = "PRIVACY CHARTER")
        }

        item {
            PrivacyCharterCard()
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Reusable UI Components
// -------------------------------------------------------------------------------------------------
@Composable
private fun CategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        lineHeight = 16.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MutedText,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MutedText,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
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
private fun EssentialAccessCard(
    essentialApps: List<EssentialApp>,
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
                modifier = Modifier.size(18.dp)
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalPhone,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(16.dp)
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

@Composable
private fun DailyAppLimitsCard(
    limits: List<AppTimeLimit>,
    installedApps: List<InstalledApp> = emptyList(),
    onAddLimitClick: () -> Unit,
    onRemoveLimit: (String) -> Unit
) {
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
                    text = "Daily App Limits",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onAddLimitClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add Limit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = "Set hard daily limits. Once an app reaches its time limit today, it is strictly blocked until midnight.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            lineHeight = 18.sp
        )

        if (limits.isEmpty()) {
            Text(
                text = "No app limits active. Tap + above to set a limit for distracting apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        } else {
            limits.forEach { limit ->
                val displayName = appsMap[limit.packageName]?.label ?: limit.packageName

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
                                text = "${limit.dailyLimitMinutes}m daily limit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onRemoveLimit(limit.packageName) },
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
                }
            }
        }
    }
}

@Composable
private fun AddTimeLimitDialog(
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, limitMinutes: Int, pinToHome: Boolean) -> Unit
) {
    var selectedPkg by remember { mutableStateOf(installedApps.firstOrNull()?.packageName ?: "") }
    var limitMinutes by remember { mutableStateOf(30) }
    var pinToHome by remember { mutableStateOf(true) }
    val presetDurations = listOf(15, 30, 45, 60, 90, 120)

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
                        onConfirm(selectedPkg, limitMinutes, pinToHome)
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
            text = "NumbPhone operates entirely local-first. Your installed applications, focus goals, usage statistics, and block logs never leave your phone. No tracking SDKs, no cloud servers, no user accounts.",
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
