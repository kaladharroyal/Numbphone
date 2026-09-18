package com.minimalphone.feature.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.ContactRepository
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.NotificationDigestRepository
import com.minimalphone.core.domain.ExtendAppTimeLimitUseCase
import com.minimalphone.core.domain.GetHomeAppsUseCase
import com.minimalphone.core.domain.SyncInstalledAppsUseCase
import com.minimalphone.core.domain.dumbmode.DumbModeManager
import com.minimalphone.core.domain.dumbmode.LauncherPolicy
import com.minimalphone.core.domain.friction.RecordIntentReflectionUseCase
import com.minimalphone.core.domain.launch.LaunchAppUseCase
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.QuickContact
import com.minimalphone.core.domain.timelimit.TimeLimitExpiryNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedDialogState(
    val isShowing: Boolean = false,
    val packageName: String = "",
    val reason: String = "",
    val activeSession: FocusSession? = null
)

data class TimeLimitExpiredDialogState(
    val isShowing: Boolean = false,
    val packageName: String = "",
    val appLabel: String = ""
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
    val isDumbMode: Boolean = false,
    val activeSession: FocusSession? = null,
    val focusGoal: String? = null,
    val remainingMinutes: Int? = null,
    val isDefaultLauncher: Boolean = false,
    val isLoading: Boolean = true,
    val blockedDialog: BlockedDialogState = BlockedDialogState(),
    val timeLimitExpiredDialog: TimeLimitExpiredDialogState = TimeLimitExpiredDialogState(),
    val reflectionDialog: ReflectionDialogState = ReflectionDialogState()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeAppsUseCase: GetHomeAppsUseCase,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase,
    private val launchAppUseCase: LaunchAppUseCase,
    private val focusSessionRepository: FocusSessionRepository,
    private val recordIntentReflectionUseCase: RecordIntentReflectionUseCase,
    private val dumbModeManager: DumbModeManager,
    private val launcherPolicy: LauncherPolicy,
    private val essentialAppRepository: EssentialAppRepository,
    private val contactRepository: ContactRepository,
    private val digestRepository: NotificationDigestRepository,
    private val extendAppTimeLimitUseCase: ExtendAppTimeLimitUseCase,
    private val timeLimitExpiryNotifier: TimeLimitExpiryNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val pinnedContacts: StateFlow<List<QuickContact>> =
        contactRepository.observePinnedContacts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val unreadDigestCount: StateFlow<Int> =
        digestRepository.observeUnreadCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    init {
        observeState()
        observeTimeLimitExpiryEvents()
        refreshApps()
    }

    private fun observeTimeLimitExpiryEvents() {
        viewModelScope.launch {
            timeLimitExpiryNotifier.currentExpiredEvent.collect { event ->
                if (event != null) {
                    _uiState.value = _uiState.value.copy(
                        timeLimitExpiredDialog = TimeLimitExpiredDialogState(
                            isShowing = true,
                            packageName = event.packageName,
                            appLabel = event.appLabel
                        )
                    )
                }
            }
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                getHomeAppsUseCase(),
                focusSessionRepository.getActiveSession(),
                dumbModeManager.isDumbModeEnabled,
                essentialAppRepository.observeEssentialPackages()
            ) { apps, session, isDumbMode, essentialPkgs ->
                val isActive = session != null && session.isCurrentlyActive
                val remainingMins = if (isActive) {
                    ((session!!.remainingMillis / 60000).toInt()).coerceAtLeast(1)
                } else null

                // In STRICT/DEEP_FOCUS mode, hide managed apps
                val focusFilteredApps = if (isActive &&
                    (session!!.mode == FocusMode.STRICT || session.mode == FocusMode.DEEP_FOCUS)
                ) {
                    apps.filter { it.category == AppCategory.ESSENTIAL || it.isEssential || essentialPkgs.contains(it.packageName) }
                } else {
                    apps
                }

                // Apply Dumb Mode policy filter
                val finalApps = launcherPolicy.filterVisibleApps(
                    apps = focusFilteredApps,
                    isDumbMode = isDumbMode,
                    essentialPackages = essentialPkgs
                )

                _uiState.value.copy(
                    visibleApps = finalApps,
                    isFocusActive = isActive,
                    isDumbMode = isDumbMode,
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
                is AppLaunchDecision.EmergencyAllow -> {
                    // Emergency / essential apps always launch immediately
                    onAllowLaunch(decision.packageName, decision.activityName)
                }
                is AppLaunchDecision.Block -> {
                    if (decision.reason.contains("Daily limit", ignoreCase = true)) {
                        timeLimitExpiryNotifier.notifyTimeLimitExpired(app.packageName, app.label)
                        _uiState.value = _uiState.value.copy(
                            timeLimitExpiredDialog = TimeLimitExpiredDialogState(
                                isShowing = true,
                                packageName = app.packageName,
                                appLabel = app.label
                            )
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            blockedDialog = BlockedDialogState(
                                isShowing = true,
                                packageName = decision.packageName,
                                reason = decision.reason,
                                activeSession = decision.activeSession
                            )
                        )
                    }
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

    fun onAddContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            contactRepository.saveContact(name, phoneNumber, isPinned = true)
        }
    }

    fun onDeleteContact(id: String) {
        viewModelScope.launch {
            contactRepository.deleteContact(id)
        }
    }

    fun extendEmergencyTime(packageName: String, additionalMinutes: Int) {
        viewModelScope.launch {
            extendAppTimeLimitUseCase(packageName, additionalMinutes)
            timeLimitExpiryNotifier.clearExpiredEvent()
            _uiState.value = _uiState.value.copy(
                timeLimitExpiredDialog = TimeLimitExpiredDialogState(isShowing = false)
            )
        }
    }

    fun dismissTimeLimitExpiredDialog() {
        timeLimitExpiryNotifier.clearExpiredEvent()
        _uiState.value = _uiState.value.copy(
            timeLimitExpiredDialog = TimeLimitExpiredDialogState(isShowing = false)
        )
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
