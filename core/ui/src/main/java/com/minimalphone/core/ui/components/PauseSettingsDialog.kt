package com.minimalphone.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalphone.core.model.PauseConfig
import com.minimalphone.core.model.PauseStyle
import com.minimalphone.core.ui.theme.DarkSurface
import com.minimalphone.core.ui.theme.MutedText
import com.minimalphone.core.ui.theme.PureBlack

private val BorderColor = Color(0xFF27272A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSettingsDialog(
    appLabel: String,
    packageName: String,
    currentConfig: PauseConfig?,
    onDismiss: () -> Unit,
    onSave: (PauseConfig) -> Unit
) {
    var isEnabled by remember { mutableStateOf(currentConfig?.enabled ?: true) }
    var pauseSeconds by remember { mutableIntStateOf(currentConfig?.pauseSeconds ?: 5) }
    var selectedStyle by remember { mutableStateOf(currentConfig?.style ?: PauseStyle.BREATHE) }
    var sessionMinutes by remember { mutableIntStateOf(currentConfig?.sessionMinutes ?: 10) }
    var reinterventionEnabled by remember { mutableStateOf(currentConfig?.reinterventionEnabled ?: true) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack, RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MINDFUL PAUSE SETTINGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MutedText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pause active toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable Pause for this App",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF32D74B),
                            uncheckedThumbColor = MutedText,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }

                if (isEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Pause Seconds Picker
                    Text(
                        text = "Pause Delay",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 10, 15).forEach { sec ->
                            val selected = pauseSeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (selected) Color.White else DarkSurface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Color.White else BorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { pauseSeconds = sec }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Session Grant Minutes Picker
                    Text(
                        text = "Grant Duration (no pause)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 5, 10, 15).forEach { mins ->
                            val selected = sessionMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (selected) Color.White else DarkSurface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Color.White else BorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { sessionMinutes = mins }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Re-intervention toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Re-intervention",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Pauses again when the session grant expires",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                        Switch(
                            checked = reinterventionEnabled,
                            onCheckedChange = { reinterventionEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF32D74B),
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = DarkSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSave(
                                PauseConfig(
                                    packageName = packageName,
                                    enabled = isEnabled,
                                    pauseSeconds = pauseSeconds,
                                    style = selectedStyle,
                                    sessionMinutes = sessionMinutes,
                                    reinterventionEnabled = reinterventionEnabled
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
