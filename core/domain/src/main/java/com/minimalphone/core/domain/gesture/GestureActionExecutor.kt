package com.minimalphone.core.domain.gesture

import com.minimalphone.core.common.AccessibilityHelper
import com.minimalphone.core.domain.launch.LaunchAppUseCase
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.GestureAction
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GestureExecutionResult {
    data object NoOp : GestureExecutionResult
    data object OpenSearch : GestureExecutionResult
    data object OpenQuickControl : GestureExecutionResult
    data object OpenSettings : GestureExecutionResult
    data object OpenFocusScreen : GestureExecutionResult
    data class Launch(val packageName: String, val decision: AppLaunchDecision) : GestureExecutionResult
    data object ScreenLocked : GestureExecutionResult
    data class ServiceNotConnected(val message: String) : GestureExecutionResult
}

@Singleton
class GestureActionExecutor @Inject constructor(
    private val launchAppUseCase: LaunchAppUseCase
) {

    suspend fun execute(action: GestureAction): GestureExecutionResult {
        return when (action) {
            is GestureAction.None -> GestureExecutionResult.NoOp
            is GestureAction.OpenSearch -> GestureExecutionResult.OpenSearch
            is GestureAction.OpenQuickControl -> GestureExecutionResult.OpenQuickControl
            is GestureAction.OpenSettings -> GestureExecutionResult.OpenSettings
            is GestureAction.OpenFocusScreen -> GestureExecutionResult.OpenFocusScreen
            is GestureAction.LaunchApp -> {
                val decision = launchAppUseCase(action.packageName)
                GestureExecutionResult.Launch(action.packageName, decision)
            }
            is GestureAction.LockScreen -> {
                val locked = AccessibilityHelper.lockScreen()
                if (locked) {
                    GestureExecutionResult.ScreenLocked
                } else {
                    GestureExecutionResult.ServiceNotConnected("Accessibility service required to lock screen")
                }
            }
        }
    }
}
