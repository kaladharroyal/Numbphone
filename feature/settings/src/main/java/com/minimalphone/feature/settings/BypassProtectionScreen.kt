package com.minimalphone.feature.settings

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.common.DeviceAdminHelper
import com.minimalphone.core.model.QuarantineHours
import com.minimalphone.core.model.SettingsGuardMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BypassProtectionScreen(
    onBack: () -> Unit,
    viewModel: BypassProtectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onAdminEnabled()
        }
    }

    LaunchedEffect(viewModel.requestEnableAdmin) {
        viewModel.requestEnableAdmin.collect {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    DeviceAdminHelper.getAdminComponentName(context)
                )
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Prevents NumbPhone from being uninstalled while Uninstall Lock is active."
                )
            }
            adminLauncher.launch(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bypass Protection", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Settings Guard ───
            GuardCard(title = "Settings Guard") {
                Text(
                    text = "Blocks access to Accessibility, App Info, and uninstallation screens while a focus session runs. Safe settings (Wi-Fi, Bluetooth, Display, Sound) remain open. Unknown settings screens are allowed and logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text("Active During:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                listOf(
                    SettingsGuardMode.OFF to "Off (Disabled)",
                    SettingsGuardMode.DEEP to "Deep Focus sessions only",
                    SettingsGuardMode.DEEP_AND_STRICT to "Deep Focus + Strict sessions"
                ).forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateSettingsGuardMode(mode) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = uiState.settingsGuardMode == mode,
                            onClick = { viewModel.updateSettingsGuardMode(mode) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // ─── Uninstall Lock ───
            GuardCard(title = "Uninstall Lock (Opt-in)") {
                Text(
                    text = "Requires deactivating Android Device Admin before NumbPhone can be uninstalled. Deactivation requests go through the exit friction escalation delay. Play Store policy compliant (empty policy).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Enable Uninstall Lock",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (uiState.isDeviceAdminActive) {
                            Text(
                                text = "Device Admin Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = uiState.uninstallLockEnabled && uiState.isDeviceAdminActive,
                        onCheckedChange = { viewModel.updateUninstallLock(it) }
                    )
                }
            }

            // ─── New-App Quarantine ───
            GuardCard(title = "New-App Quarantine") {
                Text(
                    text = "Newly installed apps are quarantined for a cooldown period to curb impulsive downloading and binge habits. Essential apps and updates bypass quarantine. Reinstalling an app restores previous rules without resetting timers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.quarantinedAppsCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${uiState.quarantinedAppsCount} app(s) currently in quarantine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Quarantine Hold Duration:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                listOf(
                    QuarantineHours.OFF to "Off (Immediate Access)",
                    QuarantineHours.ONE to "1 Hour",
                    QuarantineHours.TWENTY_FOUR to "24 Hours (Recommended)",
                    QuarantineHours.SEVENTY_TWO to "72 Hours (Strict)"
                ).forEach { (hours, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateQuarantineHours(hours) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = uiState.quarantineHours == hours,
                            onClick = { viewModel.updateQuarantineHours(hours) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuardCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}
