package com.minimalphone.core.domain.gesture

import com.minimalphone.core.common.AccessibilityHelper
import com.minimalphone.core.domain.launch.LaunchAppUseCase
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.GestureAction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GestureActionExecutorTest {

    private lateinit var launchAppUseCase: LaunchAppUseCase
    private lateinit var executor: GestureActionExecutor

    @Before
    fun setup() {
        launchAppUseCase = mockk(relaxed = true)
        executor = GestureActionExecutor(launchAppUseCase)
        mockkObject(AccessibilityHelper)
    }

    @After
    fun tearDown() {
        unmockkObject(AccessibilityHelper)
    }

    @Test
    fun `executes navigation actions correctly`() = runBlocking {
        assertEquals(GestureExecutionResult.NoOp, executor.execute(GestureAction.None))
        assertEquals(GestureExecutionResult.OpenSearch, executor.execute(GestureAction.OpenSearch))
        assertEquals(GestureExecutionResult.OpenQuickControl, executor.execute(GestureAction.OpenQuickControl))
        assertEquals(GestureExecutionResult.OpenSettings, executor.execute(GestureAction.OpenSettings))
        assertEquals(GestureExecutionResult.OpenFocusScreen, executor.execute(GestureAction.OpenFocusScreen))
    }

    @Test
    fun `executes launch app through LaunchAppUseCase`() = runBlocking {
        val pkg = "com.camera.app"
        coEvery { launchAppUseCase(pkg) } returns AppLaunchDecision.Allow(pkg, "CameraActivity")

        val result = executor.execute(GestureAction.LaunchApp(pkg))
        assertTrue(result is GestureExecutionResult.Launch)
        val launchResult = result as GestureExecutionResult.Launch
        assertEquals(pkg, launchResult.packageName)
        assertTrue(launchResult.decision is AppLaunchDecision.Allow)
    }

    @Test
    fun `executes lock screen when accessibility service is connected`() = runBlocking {
        every { AccessibilityHelper.lockScreen() } returns true

        val result = executor.execute(GestureAction.LockScreen)
        assertEquals(GestureExecutionResult.ScreenLocked, result)
    }

    @Test
    fun `returns ServiceNotConnected when accessibility service lock fails`() = runBlocking {
        every { AccessibilityHelper.lockScreen() } returns false

        val result = executor.execute(GestureAction.LockScreen)
        assertTrue(result is GestureExecutionResult.ServiceNotConnected)
    }
}
