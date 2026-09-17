package com.minimalphone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.minimalphone.core.domain.GetSettingsUseCase
import com.minimalphone.core.domain.UserSettings
import com.minimalphone.core.ui.theme.PureBlack
import com.minimalphone.feature.appslist.AppsManagementScreen
import com.minimalphone.feature.focussession.FocusSessionScreen
import com.minimalphone.feature.homescreen.HomeScreen
import com.minimalphone.feature.onboarding.OnboardingScreen
import com.minimalphone.feature.screentime.ScreenTimeScreen
import com.minimalphone.feature.settings.DiagnosticsScreen
import com.minimalphone.feature.settings.SettingsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AppsList : Screen("apps_list")
    data object FocusSession : Screen("focus_session")
    data object ScreenTime : Screen("screen_time")
    data object Diagnostics : Screen("diagnostics")
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
}

@HiltViewModel
class NavHostViewModel @Inject constructor(
    getSettingsUseCase: GetSettingsUseCase
) : ViewModel() {
    val settingsState: StateFlow<UserSettings?> = getSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}

@Composable
fun MinimalPhoneNavHost(
    viewModel: NavHostViewModel = hiltViewModel()
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    if (settings == null) {
        // Subtle loading splash while preferences load
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val currentTheme = settings?.appTheme ?: com.minimalphone.core.model.AppTheme.PURE_BLACK

    com.minimalphone.core.ui.theme.MinimalTheme(appTheme = currentTheme) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            val isFirstTime = !(settings?.isOnboardingCompleted ?: false)
            val startDestination = if (isFirstTime) Screen.Onboarding.route else Screen.Home.route
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            // Launcher Home: prevent Back button from closing the launcher
            BackHandler(enabled = true) {
                // No-op: stays on launcher home
            }

            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToFocus = {
                    navController.navigate(Screen.FocusSession.route)
                },
                onNavigateToScreenTime = {
                    navController.navigate(Screen.ScreenTime.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAppsManagement = {
                    navController.navigate(Screen.AppsList.route)
                },
                onNavigateToDiagnostics = {
                    navController.navigate(Screen.Diagnostics.route)
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.AppsList.route) {
            AppsManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.ScreenTime.route) {
            ScreenTimeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.FocusSession.route) {
            FocusSessionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
}
}
