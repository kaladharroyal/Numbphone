package com.minimalphone.feature.appslist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.ui.components.SetTimeLimitDialog
import com.minimalphone.core.ui.theme.DarkSurface
import com.minimalphone.core.ui.theme.MutedText
import com.minimalphone.core.ui.theme.PureBlack

@Composable
fun AppsManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: AppsManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedAppForLimit by remember { mutableStateOf<InstalledApp?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var isAccessibilityEnabled by remember {
        mutableStateOf(
            try {
                @Suppress("UNCHECKED_CAST")
                val serviceClass = Class.forName("com.minimalphone.service.MinimalAccessibilityService") as Class<out android.accessibilityservice.AccessibilityService>
                com.minimalphone.core.common.AccessibilityHelper.isAccessibilityServiceEnabled(context, serviceClass)
            } catch (_: Exception) {
                false
            }
        )
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
                        text = "App Rules & Classification",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = { viewModel.refreshApps() }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh apps",
                        tint = MutedText
                    )
                }
            }

            // Accessibility Permission Banner if not enabled
            if (!isAccessibilityEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(Color(0xFF332000), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFF9F0A).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            context.startActivity(com.minimalphone.core.common.AccessibilityHelper.createAccessibilitySettingsIntent())
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ Protection Service Inactive",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = Color(0xFFFFD60A)
                            )
                            Text(
                                text = "Enable Accessibility Service to allow auto-closing apps when daily limits expire.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENABLE",
                            color = Color(0xFFFFD60A),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search installed apps...", color = MutedText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MutedText
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                tint = MutedText
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.activeFilter == AppListFilter.ALL,
                    onClick = { viewModel.onFilterSelected(AppListFilter.ALL) },
                    label = { Text("All (${uiState.totalAppsCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MutedText
                    )
                )

                FilterChip(
                    selected = uiState.activeFilter == AppListFilter.HOME,
                    onClick = { viewModel.onFilterSelected(AppListFilter.HOME) },
                    label = { Text("Home Apps (${uiState.homeAppsCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MutedText
                    )
                )

                FilterChip(
                    selected = uiState.activeFilter == AppListFilter.ESSENTIAL,
                    onClick = { viewModel.onFilterSelected(AppListFilter.ESSENTIAL) },
                    label = { Text("Always Available (${uiState.essentialCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MutedText
                    )
                )

                FilterChip(
                    selected = uiState.activeFilter == AppListFilter.MANAGED,
                    onClick = { viewModel.onFilterSelected(AppListFilter.MANAGED) },
                    label = { Text("Managed (${uiState.managedCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MutedText
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Rules List / Empty state
            if (uiState.apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) "No applications matching \"${uiState.searchQuery}\"" else "No applications found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MutedText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = uiState.apps,
                        key = { it.packageName }
                    ) { app ->
                        AppManagementRow(
                            app = app,
                            timeLimit = uiState.limitsMap[app.packageName],
                            todayUsedMinutes = uiState.usageMinutesMap[app.packageName] ?: 0L,
                            onToggleCategory = { viewModel.toggleAppCategory(app) },
                            onToggleFavorite = { viewModel.toggleFavoriteOnHome(app) },
                            onConfigureLimit = { selectedAppForLimit = app }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { onNavigateToSettings() }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "NumbPhone Settings",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Text(
                                        text = "Themes, DND Mode, Portability & Suggestions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedText
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = "Settings",
                                    tint = MutedText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Daily Time Limit Configuration Dialog
        if (selectedAppForLimit != null) {
            val targetApp = selectedAppForLimit!!
            val currentLimit = uiState.limitsMap[targetApp.packageName]?.dailyLimitMinutes
            val todayUsage = uiState.usageMinutesMap[targetApp.packageName] ?: 0L

            SetTimeLimitDialog(
                appLabel = targetApp.label,
                currentLimitMinutes = currentLimit,
                todayUsedMinutes = todayUsage,
                onDismiss = { selectedAppForLimit = null },
                onSaveLimit = { limitMinutes ->
                    viewModel.setAppTimeLimit(targetApp.packageName, limitMinutes)
                },
                onRemoveLimit = {
                    viewModel.removeAppTimeLimit(targetApp.packageName)
                }
            )
        }
    }
}

@Composable
fun AppManagementRow(
    app: InstalledApp,
    timeLimit: AppTimeLimit? = null,
    todayUsedMinutes: Long = 0L,
    onToggleCategory: () -> Unit,
    onToggleFavorite: () -> Unit,
    onConfigureLimit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEssential = app.category == AppCategory.ESSENTIAL

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            val statusSubtitle = if (isEssential) "Always Available (Essential)" else "Managed (Blocked in focus)"
            val limitSubtitle = if (timeLimit != null) " • ⏳ Limit: ${timeLimit.formattedLimit} (${todayUsedMinutes}m used)" else ""
            
            Text(
                text = statusSubtitle + limitSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = if (timeLimit != null) Color(0xFFFF9F0A) else if (isEssential) Color(0xFF32D74B) else MutedText
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Daily Time Limit Button
            IconButton(onClick = onConfigureLimit) {
                Icon(
                    imageVector = Icons.Outlined.HourglassBottom,
                    contentDescription = "Set daily limit",
                    tint = if (timeLimit != null) Color(0xFFFF9F0A) else MutedText
                )
            }

            // Home Favorite Pin
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (app.isFavoriteOnHome) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Pin to home",
                    tint = if (app.isFavoriteOnHome) Color(0xFFFFD60A) else MutedText
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Always Available vs Managed Toggle
            Switch(
                checked = isEssential,
                onCheckedChange = { onToggleCategory() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MutedText,
                    uncheckedTrackColor = DarkSurface
                )
            )
        }
    }
}
