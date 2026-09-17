package com.minimalphone.feature.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.domain.GetHomeAppsUseCase
import com.minimalphone.core.domain.SyncInstalledAppsUseCase
import com.minimalphone.core.domain.friction.RecordIntentReflectionUseCase
import com.minimalphone.core.domain.launch.LaunchAppUseCase
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedDialogState(
    val isShowing: Boolean = false,
    val packageName: String = "",
    val reason: String = "",
    val activeSession: FocusSession? = null
)

data class ReflectionDialogState(
    val isShowing: Boolean = false,
    val packageName: String = "",
    val appLabel: String = "",
    val focusSessionId: String? = null
)

data class HomeUiState(
    val visibleApps: List<InstalledApp> = emptyList(),
    val isFocusActive: Boolean = false,
    val activeSession: FocusSession? = null,
    val focusGoal: String? = null,
    val remainingMinutes: Int? = null,
    val isDefaultLauncher: Boolean = false,
    val isLoading: Boolean = true,
    val blockedDialog: BlockedDialogState = BlockedDialogState(),
    val reflectionDialog: ReflectionDialogState = ReflectionDialogState()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeAppsUseCase: GetHomeAppsUseCase,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase,
    private val launchAppUseCase: LaunchAppUseCase,
    private val focusSessionRepository: FocusSessionRepository,
    private val recordIntentReflectionUseCase: RecordIntentReflectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeState()
        refreshApps()
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                getHomeAppsUseCase(),
                focusSessionRepository.getActiveSession()
            ) { apps, session ->
                val isActive = session != null && session.isCurrentlyActive
                val remainingMins = if (isActive) {
                    ((session!!.remainingMillis / 60000).toInt()).coerceAtLeast(1)
                } else null

                // In STRICT and DEEP_FOCUS mode, hide managed apps from the home screen
                val filteredApps = if (isActive &&
                    (session!!.mode == FocusMode.STRICT || session.mode == FocusMode.DEEP_FOCUS)
                ) {
                    apps.filter { it.category == AppCategory.ESSENTIAL || it.isEssential }
                } else {
                    apps
                }

                _uiState.value.copy(
                    visibleApps = filteredApps,
                    isFocusActive = isActive,
                    activeSession = session,
                    focusGoal = session?.goal?.title,
                    remainingMinutes = remainingMins,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onAppClicked(app: InstalledApp, onAllowLaunch: (String, String) -> Unit) {
        viewModelScope.launch {
            when (val decision = launchAppUseCase(app.packageName)) {
                is AppLaunchDecision.Allow -> {
                    onAllowLaunch(decision.packageName, decision.activityName)
                }
                is AppLaunchDecision.Block -> {
                    _uiState.value = _uiState.value.copy(
                        blockedDialog = BlockedDialogState(
                            isShowing = true,
                            packageName = decision.packageName,
                            reason = decision.reason,
                            activeSession = decision.activeSession
                        )
                    )
                }
                is AppLaunchDecision.ShowFriction -> {
                    _uiState.value = _uiState.value.copy(
                        reflectionDialog = ReflectionDialogState(
                            isShowing = true,
                            packageName = app.packageName,
                            appLabel = app.label,
                            focusSessionId = decision.activeSession.id
                        )
                    )
                }
            }
        }
    }

    fun onRecordReflection(reason: String) {
        viewModelScope.launch {
            val state = _uiState.value.reflectionDialog
            if (state.packageName.isNotBlank()) {
                recordIntentReflectionUseCase(
                    packageName = state.packageName,
                    appLabel = state.appLabel,
                    focusSessionId = state.focusSessionId,
                    userIntentReason = reason
                )
            }
            _uiState.value = _uiState.value.copy(
                reflectionDialog = ReflectionDialogState(isShowing = false)
            )
        }
    }

    fun dismissReflectionDialog() {
        _uiState.value = _uiState.value.copy(
            reflectionDialog = ReflectionDialogState(isShowing = false)
        )
    }

    fun dismissBlockedDialog() {
        _uiState.value = _uiState.value.copy(
            blockedDialog = BlockedDialogState(isShowing = false)
        )
    }

    fun refreshApps() {
        viewModelScope.launch {
            syncInstalledAppsUseCase()
        }
    }
}
