package com.minimalphone.feature.homescreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolumeMute
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
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
import com.minimalphone.core.common.AudioMode
import com.minimalphone.core.common.DndHelper
import com.minimalphone.core.common.QuickControlsHelper
import com.minimalphone.core.ui.theme.DarkSurface
import com.minimalphone.core.ui.theme.MutedText
import com.minimalphone.core.ui.theme.PureBlack
import com.minimalphone.core.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TileBorder = Color(0xFF27272A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalControlPanelSheet(
    onDismiss: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToScreenTime: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isTorchOn by remember { mutableStateOf(QuickControlsHelper.isTorchOn()) }
    var audioMode by remember { mutableStateOf(QuickControlsHelper.getAudioMode(context)) }
    var isDndActive by remember { mutableStateOf(false) }
    val isDndGranted = remember { DndHelper.isNotificationPolicyAccessGranted(context) }
    val batteryInfo = remember { QuickControlsHelper.getBatteryInfo(context) }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val now = remember { System.currentTimeMillis() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0xFF3F3F46), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Live Status + Battery Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = timeFormat.format(Date(now)),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = dateFormat.format(Date(now)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }

                // Battery Badge
                Row(
                    modifier = Modifier
                        .background(DarkSurface, RoundedCornerShape(20.dp))
                        .border(1.dp, TileBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (batteryInfo.isCharging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryStd,
                        contentDescription = null,
                        tint = if (batteryInfo.percentage <= 20) Color(0xFFFF453A) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${batteryInfo.percentage}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Quick Control Center Tiles
            Text(
                text = "CONTROL CENTER",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MutedText,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tile 1: DND Mode
                item {
                    ControlTile(
                        icon = if (isDndActive) Icons.Outlined.NotificationsOff else Icons.Outlined.DoNotDisturbOn,
                        label = "DND Mode",
                        status = if (isDndActive) "ON" else "OFF",
                        isActive = isDndActive,
                        onClick = {
                            if (isDndGranted) {
                                if (isDndActive) {
                                    DndHelper.restoreNormalNotifications(context)
                                    isDndActive = false
                                } else {
                                    DndHelper.enablePriorityCallsOnlyDnd(context)
                                    isDndActive = true
                                }
                            } else {
                                Toast.makeText(context, "Grant DND Permission first", Toast.LENGTH_SHORT).show()
                                DndHelper.openNotificationPolicySettings(context)
                            }
                        }
                    )
                }

                // Tile 2: Flashlight / Torch
                item {
                    ControlTile(
                        icon = if (isTorchOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                        label = "Flashlight",
                        status = if (isTorchOn) "ON" else "OFF",
                        isActive = isTorchOn,
                        onClick = {
                            isTorchOn = QuickControlsHelper.toggleTorch(context)
                        }
                    )
                }

                // Tile 3: Sound Mode
                item {
                    val soundIcon = when (audioMode) {
                        AudioMode.NORMAL -> Icons.Outlined.VolumeUp
                        AudioMode.VIBRATE -> Icons.Outlined.VolumeMute
                        AudioMode.SILENT -> Icons.Outlined.VolumeOff
                    }
                    ControlTile(
                        icon = soundIcon,
                        label = "Sound",
                        status = audioMode.label,
                        isActive = audioMode != AudioMode.SILENT,
                        onClick = {
                            audioMode = QuickControlsHelper.cycleAudioMode(context)
                        }
                    )
                }

                // Tile 4: Wi-Fi Shortcut
                item {
                    ControlTile(
                        icon = Icons.Outlined.Wifi,
                        label = "Wi-Fi",
                        status = "Settings",
                        isActive = false,
                        onClick = {
                            QuickControlsHelper.openWifiSettings(context)
                            onDismiss()
                        }
                    )
                }

                // Tile 5: Bluetooth Shortcut
                item {
                    ControlTile(
                        icon = Icons.Outlined.Bluetooth,
                        label = "Bluetooth",
                        status = "Settings",
                        isActive = false,
                        onClick = {
                            QuickControlsHelper.openBluetoothSettings(context)
                            onDismiss()
                        }
                    )
                }

                // Tile 6: Screen Time Analytics
                item {
                    ControlTile(
                        icon = Icons.Outlined.HourglassBottom,
                        label = "Screen Time",
                        status = "Usage Stats",
                        isActive = false,
                        onClick = {
                            onNavigateToScreenTime()
                            onDismiss()
                        }
                    )
                }

                // Tile 7: Start Focus Session
                item {
                    ControlTile(
                        icon = Icons.Outlined.SelfImprovement,
                        label = "Focus Session",
                        status = "Protect Flow",
                        isActive = false,
                        onClick = {
                            onNavigateToFocus()
                            onDismiss()
                        }
                    )
                }

                // Tile 8: System Settings
                item {
                    ControlTile(
                        icon = Icons.Outlined.Settings,
                        label = "Settings",
                        status = "Device & App",
                        isActive = false,
                        onClick = {
                            onNavigateToSettings()
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Distraction Shield & Notification Filter
            Text(
                text = "NOTIFICATION SHIELD",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MutedText,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, TileBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isDndActive) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = if (isDndActive) SuccessGreen else MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isDndActive) "Focus Shield Active" else "Notification Shield Ready",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = if (isDndActive) {
                            "Distracting notifications and visual popups are silenced. Important calls remain reachable."
                        } else {
                            "DND can eliminate incoming popups, vibrations, and notification pings while you work."
                        },
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                QuickControlsHelper.openNotificationSettings(context)
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "Notification Settings →",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlTile(
    icon: ImageVector,
    label: String,
    status: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) Color.White else DarkSurface,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (isActive) Color.White else TileBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) PureBlack else Color.White,
                modifier = Modifier.size(22.dp)
            )

            Column {
                Text(
                    text = label,
                    color = if (isActive) PureBlack else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = status,
                    color = if (isActive) Color(0xFF3F3F46) else MutedText,
                    fontSize = 11.sp
                )
            }
        }
    }
}
