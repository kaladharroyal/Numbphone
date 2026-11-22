package com.minimalphone.feature.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.ContactRepository
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.NotificationDigestRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.domain.dumbmode.LauncherPolicy
import com.minimalphone.core.domain.friction.RecordIntentReflectionUseCase
import com.minimalphone.core.domain.launch.LaunchAppUseCase
import com.minimalphone.core.domain.pause.PauseEngine
import com.minimalphone.core.domain.timelimit.TimeLimitExpiryNotifier
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.AutoLaunchMode
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.GestureAction
import com.minimalphone.core.model.GestureType
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.PauseConfig
import com.minimalphone.core.model.PauseEventKind
import com.minimalphone.core.model.PauseOutcome
import com.minimalphone.core.model.QuickContact
import com.minimalphone.core.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val activityName: String = "",
    val focusSessionId: String? = null
)

data class PauseScreenState(
    val isShowing: Boolean = false,
    val packageName: String = "",
    val appLabel: String = "",
    val activityName: String = "",
    val config: PauseConfig = PauseConfig(""),
    val delaySeconds: Int = 5
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
    val reflectionDialog: ReflectionDialogState = ReflectionDialogState(),
    val pauseScreen: PauseScreenState = PauseScreenState(),
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<com.minimalphone.core.model.SearchResult> = emptyList(),
    val autoLaunchCountdownApp: com.minimalphone.core.model.SearchResult.App? = null,
    val gestureHintShown: Boolean = true,
    val limitsMap: Map<String, AppTimeLimit> = emptyMap(),
    val pauseConfigsMap: Map<String, PauseConfig> = emptyMap()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val launchAppUseCase: LaunchAppUseCase,
    private val focusSessionRepository: FocusSessionRepository,
    private val recordIntentReflectionUseCase: RecordIntentReflectionUseCase,
    private val settingsRepository: SettingsRepository,
    private val launcherPolicy: LauncherPolicy,
    private val essentialAppRepository: EssentialAppRepository,
    private val contactRepository: ContactRepository,
    private val digestRepository: NotificationDigestRepository,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val timeLimitExpiryNotifier: TimeLimitExpiryNotifier,
    private val pauseEngine: PauseEngine? = null,
    private val searchAppsUseCase: com.minimalphone.core.domain.search.SearchAppsUseCase? = null,
    private val gestureActionExecutor: com.minimalphone.core.domain.gesture.GestureActionExecutor? = null,
    private val pauseRepository: com.minimalphone.core.data.repository.PauseRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var autoLaunchJob: kotlinx.coroutines.Job? = null
    private var previousQueryLength = 0
    val pendingAutoLaunchApp = MutableStateFlow<com.minimalphone.core.model.SearchResult.App?>(null)

    val gestureConfig: StateFlow<Map<com.minimalphone.core.model.GestureType, com.minimalphone.core.model.GestureAction>> =
        settingsRepository.gestureConfig
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = com.minimalphone.core.model.GestureAction.defaultGestureConfig()
            )

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
        observeSearch()
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
                combine(appRepository.getHomeApps(), focusSessionRepository.getActiveSession()) { a, s -> Pair(a, s) },
                combine(settingsRepository.isDumbModeEnabled, essentialAppRepository.observeEssentialPackages(), settingsRepository.gestureHintShown) { d, e, h -> Triple(d, e, h) },
                appTimeLimitRepository.observeAllLimits(),
                pauseRepository?.observeAllConfigs() ?: flowOf(emptyList())
            ) { (apps, session), (isDumbMode, essentialPkgs, gestureHintShown), limits, pauseConfigs ->
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
                    isLoading = false,
                    gestureHintShown = gestureHintShown,
                    limitsMap = limits.associateBy { it.packageName },
                    pauseConfigsMap = pauseConfigs.associateBy { it.packageName }
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSearch() {
        if (searchAppsUseCase == null) return
        viewModelScope.launch {
            _searchQuery
                .debounce(50L)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        searchAppsUseCase.search(query)
                    }
                }
                .collect { results ->
                    _uiState.value = _uiState.value.copy(searchResults = results)
                    handleAutoLaunch(results)
                }
        }
    }

    fun openSearch() {
        cancelAutoLaunch()
        _searchQuery.value = ""
        previousQueryLength = 0
        _uiState.value = _uiState.value.copy(
            isSearchOpen = true,
            searchQuery = "",
            searchResults = emptyList(),
            autoLaunchCountdownApp = null
        )
    }

    fun closeSearch() {
        cancelAutoLaunch()
        _searchQuery.value = ""
        previousQueryLength = 0
        _uiState.value = _uiState.value.copy(
            isSearchOpen = false,
            searchQuery = "",
            searchResults = emptyList(),
            autoLaunchCountdownApp = null
        )
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun cancelAutoLaunch() {
        autoLaunchJob?.cancel()
        autoLaunchJob = null
        if (_uiState.value.autoLaunchCountdownApp != null) {
            _uiState.value = _uiState.value.copy(autoLaunchCountdownApp = null)
        }
    }

    private fun handleAutoLaunch(results: List<SearchResult>) {
        val currentQuery = _uiState.value.searchQuery
        val isDeleting = currentQuery.length < previousQueryLength
        previousQueryLength = currentQuery.length

        if (isDeleting || results.size != 1) {
            cancelAutoLaunch()
            return
        }

        val singleResult = results.first()
        if (singleResult !is SearchResult.App) {
            cancelAutoLaunch()
            return
        }

        val app = singleResult.installedApp
        viewModelScope.launch {
            val mode = settingsRepository.searchAutoLaunchMode.first()
            val shouldAutoLaunch = when (mode) {
                AutoLaunchMode.OFF -> false
                AutoLaunchMode.ESSENTIAL_AND_PINNED -> app.isEssential || app.category == AppCategory.ESSENTIAL || app.isFavoriteOnHome
                AutoLaunchMode.ALL -> true
            }

            if (shouldAutoLaunch) {
                _uiState.value = _uiState.value.copy(autoLaunchCountdownApp = singleResult)
                autoLaunchJob?.cancel()
                autoLaunchJob = viewModelScope.launch {
                    delay(500L)
                    if (_uiState.value.autoLaunchCountdownApp == singleResult) {
                        _uiState.value = _uiState.value.copy(
                            autoLaunchCountdownApp = null,
                            isSearchOpen = false
                        )
                        pendingAutoLaunchApp.value = singleResult
                    }
                }
            } else {
                cancelAutoLaunch()
            }
        }
    }

    fun onLaunchSearchResult(
        result: SearchResult,
        onAllowLaunch: (String, String) -> Unit,
        onDialNumber: (String) -> Unit
    ) {
        closeSearch()
        when (result) {
            is SearchResult.App -> {
                onAppClicked(result.installedApp, onAllowLaunch)
            }
            is SearchResult.QuickContact -> {
                onDialNumber(result.contact.phoneNumber)
            }
            is SearchResult.DialNumber -> {
                onDialNumber(result.number)
            }
        }
    }

    fun onGestureRecognized(
        type: GestureType,
        onNavigateToControlCenter: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToFocus: () -> Unit,
        onAllowLaunch: (String, String) -> Unit,
        onShowMessage: (String) -> Unit
    ) {
        val action = gestureConfig.value[type] ?: GestureAction.None
        viewModelScope.launch {
            if (gestureActionExecutor != null) {
                when (val result = gestureActionExecutor.execute(action)) {
                    is com.minimalphone.core.domain.gesture.GestureExecutionResult.NoOp -> { /* No-op */ }
                    is com.minimalphone.core.domain.gesture.GestureExecutionResult.OpenSearch -> {
                        openSearch()
                    }
                    is com.minimalphone.core.domain.gesture.GestureExecutionResult.OpenQuickControl -> {
                        onNavigateToControlCenter()
                    }
                    is com.minimalphone.core.domain.gesture.GestureExecutionResult.OpenSettings -> {
                        onNavigateToSettings()
                    }
                    is com.minimalphone.core.domain.gesture.GestureExecutionResult.OpenFocusScreen -> {
                        onNavigateToFocus()
                    }
                    is com.minimalphone.core.domain.gesture.GestureExecutionResult.Launch -> {
                        val app = appRepository.getApp(result.packageName)
                            ?: InstalledApp(packageName = result.packageName, activityName = "", label = result.packageName)
                        onAppClicked(app, onAllowLaunch)
                    }
                    is com.minimalphone.core.domain.gesture.GestureExecutionResult.ScreenLocked -> { /* Screen locked */ }
                    is com.minimalphone.core.domain.gesture.GestureExecutionResult.ServiceNotConnected -> {
                        onShowMessage(result.message)
                    }
                }
            } else {
                // Default fallback if executor is not provided
                when (action) {
                    is GestureAction.OpenSearch -> openSearch()
                    is GestureAction.OpenQuickControl -> onNavigateToControlCenter()
                    is GestureAction.OpenSettings -> onNavigateToSettings()
                    is GestureAction.OpenFocusScreen -> onNavigateToFocus()
                    is GestureAction.LaunchApp -> {
                        val app = appRepository.getApp(action.packageName)
                            ?: InstalledApp(packageName = action.packageName, activityName = "", label = action.packageName)
                        onAppClicked(app, onAllowLaunch)
                    }
                    is GestureAction.LockScreen -> {
                        val locked = com.minimalphone.core.common.AccessibilityHelper.lockScreen()
                        if (!locked) {
                            onShowMessage("Accessibility service required to lock screen")
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun dismissGestureHint() {
        viewModelScope.launch {
            settingsRepository.setGestureHintShown(true)
        }
    }

    fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) {
        viewModelScope.launch {
            appRepository.toggleFavoriteOnHome(packageName, isFavorite)
        }
    }

    suspend fun getTimeLimitForPackage(packageName: String): AppTimeLimit? {
        return appTimeLimitRepository.getLimit(packageName)
    }

    suspend fun getPauseConfigForPackage(packageName: String): PauseConfig? {
        return pauseRepository?.getAppPauseConfigSync(packageName)
    }

    fun saveTimeLimit(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            appTimeLimitRepository.setLimit(packageName, limitMinutes, isEnabled = true)
        }
    }

    fun removeTimeLimit(packageName: String) {
        viewModelScope.launch {
            appTimeLimitRepository.removeLimit(packageName)
        }
    }

    fun savePauseConfig(config: PauseConfig) {
        viewModelScope.launch {
            pauseRepository?.saveAppPauseConfig(config)
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
                            activityName = app.activityName,
                            focusSessionId = decision.activeSession.id
                        )
                    )
                }
                is AppLaunchDecision.ShowPause -> {
                    _uiState.value = _uiState.value.copy(
                        pauseScreen = PauseScreenState(
                            isShowing = true,
                            packageName = decision.packageName,
                            appLabel = app.label,
                            activityName = decision.activityName,
                            config = decision.config,
                            delaySeconds = decision.delaySeconds
                        )
                    )
                }
            }
        }
    }

    fun onPauseLeave(packageName: String, delaySeconds: Int) {
        _uiState.value = _uiState.value.copy(pauseScreen = PauseScreenState(isShowing = false))
        viewModelScope.launch {
            pauseEngine?.recordOutcome(
                packageName = packageName,
                kind = PauseEventKind.LAUNCH,
                outcome = PauseOutcome.LEFT,
                secondsShown = delaySeconds
            )
        }
    }

    fun onPauseContinue(
        packageName: String,
        activityName: String,
        config: PauseConfig,
        delaySeconds: Int,
        reflectionReason: String?,
        onAllowLaunch: (String, String) -> Unit
    ) {
        _uiState.value = _uiState.value.copy(pauseScreen = PauseScreenState(isShowing = false))
        pauseEngine?.grantSession(packageName, config.sessionMinutes)
        viewModelScope.launch {
            pauseEngine?.recordOutcome(
                packageName = packageName,
                kind = PauseEventKind.LAUNCH,
                outcome = PauseOutcome.PROCEEDED,
                reflectionReason = reflectionReason,
                secondsShown = delaySeconds
            )
        }
        onAllowLaunch(packageName, activityName)
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
            if (additionalMinutes > 0) {
                appTimeLimitRepository.addEmergencyExtension(packageName, additionalMinutes)
            }
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

    fun onRecordReflection(
        reason: String,
        onLaunchApproved: ((packageName: String, activityName: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val state = _uiState.value.reflectionDialog
            val pkg = state.packageName
            val act = state.activityName
            if (pkg.isNotBlank()) {
                recordIntentReflectionUseCase(
                    packageName = pkg,
                    appLabel = state.appLabel,
                    focusSessionId = state.focusSessionId,
                    userIntentReason = reason
                )
            }
            _uiState.value = _uiState.value.copy(
                reflectionDialog = ReflectionDialogState(isShowing = false)
            )
            if (pkg.isNotBlank()) {
                onLaunchApproved?.invoke(pkg, act)
            }
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
            appRepository.syncInstalledApps()
        }
    }
}
