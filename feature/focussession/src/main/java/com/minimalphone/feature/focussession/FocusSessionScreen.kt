package com.minimalphone.feature.focussession

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusPreset
import com.minimalphone.core.model.FocusSchedule
import com.minimalphone.core.ui.components.ExitFrictionDialog
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Composable
fun FocusSessionScreen(
    onNavigateBack: () -> Unit,
    viewModel: FocusSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isSessionActive) {
            ActiveFocusSessionContent(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onEndSession = {
                    viewModel.onRequestExitSession()
                }
            )

            // Adaptive Exit Friction Dialog
            val pendingExit = uiState.pendingExitAttempt
            if (pendingExit != null) {
                ExitFrictionDialog(
                    pendingExit = pendingExit,
                    onContinueFocus = { secondsWaited ->
                        viewModel.onAbortExitAttempt(secondsWaited)
                    },
                    onExitAnyway = { completedSeconds ->
                        viewModel.onConfirmExitAttempt(completedSeconds)
                    }
                )
            }
        } else {
            SetupFocusSessionContent(
                uiState = uiState,
                presets = presets,
                schedules = schedules,
                onNavigateBack = onNavigateBack,
                onSelectDuration = viewModel::onSelectDuration,
                onSelectMode = viewModel::onSelectMode,
                onSelectGoal = viewModel::onSelectGoal,
                onSelectPreset = viewModel::onSelectPreset,
                onToggleSchedule = viewModel::onToggleSchedule,
                onSaveSchedule = viewModel::onSaveSchedule,
                onDeleteSchedule = viewModel::onDeleteSchedule,
                onSelectCustomGoal = viewModel::onSelectCustomGoal,
                onCustomGoalTextChanged = viewModel::onCustomGoalTextChanged,
                onStartSession = viewModel::onStartFocusSession,
                onDismissError = viewModel::dismissError
            )
        }
    }
}

@Composable
private fun ActiveFocusSessionContent(
    uiState: FocusSessionUiState,
    onNavigateBack: () -> Unit,
    onEndSession: () -> Unit
) {
    val ticker = uiState.ticker
    val session = uiState.activeSession
    val goalTitle = session?.goal?.title ?: "Focus Session"
    val modeName = when (session?.mode) {
        FocusMode.LIGHT -> "LIGHT MODE"
        FocusMode.DEEP_FOCUS -> "DEEP FOCUS"
        else -> "STRICT MODE"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Minimize to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ACTIVE FOCUS SESSION",
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Large Minimalist Digital Clock
        Text(
            text = ticker.formattedTime,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Linear Progress bar
        LinearProgressIndicator(
            progress = { ticker.progress },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(3.dp),
            color = MaterialTheme.colorScheme.onBackground,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Focus Goal Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CURRENT GOAL",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = goalTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = modeName,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (session?.mode == FocusMode.LIGHT) {
                "Managed apps show a friction countdown before launch."
            } else {
                "Managed apps are hidden from the home screen and blocked."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // End / Leave session button
        Button(
            onClick = onEndSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text(
                text = "END FOCUS SESSION",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupFocusSessionContent(
    uiState: FocusSessionUiState,
    presets: List<FocusPreset>,
    schedules: List<FocusSchedule>,
    onNavigateBack: () -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectMode: (FocusMode) -> Unit,
    onSelectGoal: (FocusGoal) -> Unit,
    onSelectPreset: (FocusPreset) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    onSaveSchedule: (FocusSchedule) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onSelectCustomGoal: () -> Unit,
    onCustomGoalTextChanged: (String) -> Unit,
    onStartSession: () -> Unit,
    onDismissError: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "FOCUS SESSION",
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Set your intention and eliminate distractions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 0: ONE-TAP PRESETS
        if (presets.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "QUICK FOCUS PRESETS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${preset.title} • ${preset.durationMinutes}m",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // SECTION 1: GOAL SELECTION
        Text(
            text = "WHAT ARE YOU FOCUSING ON?",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            uiState.availableGoals.forEach { goal ->
                val isSelected = !uiState.isCustomGoalSelected && uiState.selectedGoal?.id == goal.id
                FocusChip(
                    text = goal.title,
                    isSelected = isSelected,
                    onClick = { onSelectGoal(goal) }
                )
            }

            FocusChip(
                text = "+ Custom Goal",
                isSelected = uiState.isCustomGoalSelected,
                onClick = onSelectCustomGoal
            )
        }

        if (uiState.isCustomGoalSelected) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.customGoalText,
                onValueChange = onCustomGoalTextChanged,
                placeholder = {
                    Text(
                        text = "e.g. Complete Machine Learning Unit 3",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION 2: DURATION SELECTION
        Text(
            text = "DURATION",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        val durationOptions = listOf(
            15 to "15 min",
            25 to "25 min",
            45 to "45 min",
            60 to "60 min",
            90 to "90 min",
            120 to "2 hours"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            durationOptions.forEach { (minutes, label) ->
                val isSelected = uiState.selectedDurationMinutes == minutes
                FocusChip(
                    text = label,
                    isSelected = isSelected,
                    onClick = { onSelectDuration(minutes) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION 3: FOCUS MODE SELECTION
        Text(
            text = "FOCUS LEVEL",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        ModeSelectionCard(
            title = "STRICT FOCUS",
            subtitle = "Managed apps hidden from launcher & blocked. Recommended.",
            isSelected = uiState.selectedMode == FocusMode.STRICT,
            onClick = { onSelectMode(FocusMode.STRICT) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        ModeSelectionCard(
            title = "LIGHT FOCUS",
            subtitle = "Managed apps accessible after a short deliberate friction pause.",
            isSelected = uiState.selectedMode == FocusMode.LIGHT,
            onClick = { onSelectMode(FocusMode.LIGHT) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        ModeSelectionCard(
            title = "DEEP FOCUS",
            subtitle = "Maximum lockdown. No distraction bypass, hidden apps.",
            isSelected = uiState.selectedMode == FocusMode.DEEP_FOCUS,
            onClick = { onSelectMode(FocusMode.DEEP_FOCUS) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION 4: RECURRING SCHEDULES
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
                    imageVector = Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "RECURRING SCHEDULES",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            IconButton(onClick = { showAddScheduleDialog = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Schedule",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (schedules.isEmpty()) {
            Text(
                text = "No automated schedules yet. Tap + to set a recurring focus window (e.g. Work 9am-5pm).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        } else {
            schedules.forEach { schedule ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = schedule.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${schedule.startTime} (${schedule.durationMinutes}m) • ${schedule.daysOfWeek.joinToString { it.name.take(3) }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = schedule.isEnabled,
                                onCheckedChange = { onToggleSchedule(schedule.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(onClick = { onDeleteSchedule(schedule.id) }) {
                                Text("✕", color = Color(0xFFFF453A))
                            }
                        }
                    }
                }
            }
        }

        // Error message if any
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.errorMessage ?: "",
                color = Color(0xFFE57373),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // BEGIN FOCUS BUTTON
        Button(
            onClick = onStartSession,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.background,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "BEGIN FOCUS SESSION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { schedule ->
                onSaveSchedule(schedule)
                showAddScheduleDialog = false
            }
        )
    }
}

@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (FocusSchedule) -> Unit
) {
    var title by remember { mutableStateOf("Work Hours Focus") }
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    var durationMinutes by remember { mutableStateOf(60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "New Focus Schedule",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Schedule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hour.toString(),
                        onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                        label = { Text("Hour (0-23)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minute.toString(),
                        onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                        label = { Text("Min (0-59)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = durationMinutes.toString(),
                    onValueChange = { durationMinutes = it.toIntOrNull()?.coerceIn(15, 720) ?: 60 },
                    label = { Text("Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val schedule = FocusSchedule(
                            id = "sched_${UUID.randomUUID()}",
                            title = title.trim(),
                            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                            startTime = LocalTime.of(hour, minute),
                            durationMinutes = durationMinutes,
                            mode = FocusMode.STRICT,
                            isEnabled = true
                        )
                        onConfirm(schedule)
                    }
                }
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FocusChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color.Transparent
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
    }

    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = textColor
        )
    }
}

@Composable
private fun ModeSelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
