package com.minimalphone.feature.homescreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.PauseConfig
import com.minimalphone.core.model.SearchResult
import com.minimalphone.core.ui.components.MinimalAppItemRow
import com.minimalphone.core.ui.components.PauseSettingsDialog
import com.minimalphone.core.ui.components.SetTimeLimitDialog
import com.minimalphone.core.ui.theme.DarkSurface
import com.minimalphone.core.ui.theme.MutedText
import com.minimalphone.core.ui.theme.PureBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSearchOverlay(
    query: String,
    results: List<SearchResult>,
    autoLaunchCountdownApp: SearchResult.App?,
    onQueryChange: (String) -> Unit,
    onCancelAutoLaunch: () -> Unit,
    onLaunchResult: (SearchResult) -> Unit,
    onToggleFavorite: (packageName: String, isFavorite: Boolean) -> Unit,
    onSaveTimeLimit: (packageName: String, limitMinutes: Int) -> Unit,
    onRemoveTimeLimit: (packageName: String) -> Unit,
    onSavePauseConfig: (PauseConfig) -> Unit,
    getTimeLimitForPackage: (String) -> AppTimeLimit?,
    getPauseConfigForPackage: (String) -> PauseConfig?,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedAppForMenu by remember { mutableStateOf<SearchResult.App?>(null) }
    var appForTimeLimitDialog by remember { mutableStateOf<SearchResult.App?>(null) }
    var appForPauseSettingsDialog by remember { mutableStateOf<SearchResult.App?>(null) }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (autoLaunchCountdownApp != null) {
                    onCancelAutoLaunch()
                } else {
                    onDismiss()
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) {
                        onDismiss()
                    }
                }
            }
            .imePadding()
    ) {
        // Bottom-up layout: results sit directly above the keyboard
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (autoLaunchCountdownApp != null) {
                        onCancelAutoLaunch()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Auto-launch countdown badge if single match
            AnimatedVisibility(
                visible = autoLaunchCountdownApp != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (autoLaunchCountdownApp != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onCancelAutoLaunch() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Opening ${autoLaunchCountdownApp.displayLabel}…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap to cancel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                    }
                }
            }

            // Results List (at most 6, rendered bottom-up)
            if (results.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = false
                ) {
                    items(results, key = { it.id }) { result ->
                        when (result) {
                            is SearchResult.App -> {
                                val secondaryBadge = when {
                                    result.isQuarantined -> result.quarantineBadgeText
                                    result.installedApp.isClone -> "(clone)"
                                    else -> null
                                }

                                MinimalAppItemRow(
                                    label = result.displayLabel,
                                    trailingText = secondaryBadge,
                                    onClick = {
                                        onLaunchResult(result)
                                    },
                                    onLongClick = {
                                        selectedAppForMenu = result
                                    }
                                )
                            }
                            is SearchResult.QuickContact -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onLaunchResult(result) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Call,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = result.contact.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = result.contact.phoneNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedText
                                        )
                                    }
                                }
                            }
                            is SearchResult.DialNumber -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onLaunchResult(result) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Dialpad,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = result.displayLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (query.isNotBlank()) {
                Text(
                    text = "No apps found",
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
                )
            }

            // Search Input Row with Instant Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "Search apps and contacts" },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val topResult = results.firstOrNull()
                            if (topResult != null) {
                                onLaunchResult(topResult)
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MutedText
                            )
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear search",
                            tint = MutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Long Press Action Menu for App Results
    if (selectedAppForMenu != null) {
        val appResult = selectedAppForMenu!!
        val app = appResult.installedApp
        val isFavorite = app.isFavoriteOnHome

        BasicAlertDialog(
            onDismissRequest = { selectedAppForMenu = null }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = appResult.displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Pin or Unpin
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleFavorite(app.packageName, !isFavorite)
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isFavorite) "Unpin from home" else "Pin to home",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Set Time Limit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                appForTimeLimitDialog = appResult
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Set daily time limit",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Mindful Pause Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                appForPauseSettingsDialog = appResult
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Mindful pause settings",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedAppForMenu = null }) {
                            Text("Cancel", color = MutedText)
                        }
                    }
                }
            }
        }
    }

    // Set Time Limit Dialog
    if (appForTimeLimitDialog != null) {
        val target = appForTimeLimitDialog!!
        val currentLimit = getTimeLimitForPackage(target.installedApp.packageName)
        SetTimeLimitDialog(
            appLabel = target.displayLabel,
            currentLimitMinutes = currentLimit?.dailyLimitMinutes,
            todayUsedMinutes = 0L,
            onDismiss = { appForTimeLimitDialog = null },
            onSaveLimit = { minutes ->
                onSaveTimeLimit(target.installedApp.packageName, minutes)
                appForTimeLimitDialog = null
            },
            onRemoveLimit = {
                onRemoveTimeLimit(target.installedApp.packageName)
                appForTimeLimitDialog = null
            }
        )
    }

    // Mindful Pause Settings Dialog
    if (appForPauseSettingsDialog != null) {
        val target = appForPauseSettingsDialog!!
        val currentConfig = getPauseConfigForPackage(target.installedApp.packageName)
        PauseSettingsDialog(
            appLabel = target.displayLabel,
            packageName = target.installedApp.packageName,
            currentConfig = currentConfig,
            onDismiss = { appForPauseSettingsDialog = null },
            onSave = { config ->
                onSavePauseConfig(config)
                appForPauseSettingsDialog = null
            }
        )
    }
}
