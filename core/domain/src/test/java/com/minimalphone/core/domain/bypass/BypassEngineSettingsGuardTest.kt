package com.minimalphone.core.domain.bypass

import com.minimalphone.core.model.SettingsGuardDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BypassEngineSettingsGuardTest {

    private lateinit var engine: SettingsGuardEngine
    private lateinit var config: SettingsGuardConfig

    @Before
    fun setup() {
        engine = SettingsGuardEngine()
        config = SettingsGuardConfig(
            blockedClasses = listOf(
                "AccessibilitySettings",
                "InstalledAppDetails",
                "AppInfoDashboardFragment",
                "DeviceAdminAdd",
                "UninstallerActivity",
                "PackageInstallerActivity"
            ),
            allowedClasses = listOf(
                "WifiSettings",
                "BluetoothSettings",
                "DisplaySettings",
                "SoundSettings"
            ),
            settingsPackages = listOf(
                "com.android.settings",
                "com.android.packageinstaller"
            )
        )
    }

    @Test
    fun `accessibility settings class is blocked`() {
        val result = engine.evaluate(
            packageName = "com.android.settings",
            className = "com.android.settings.accessibility.AccessibilitySettings",
            config = config
        )
        assertTrue(result is SettingsGuardDecision.Block)
        assertEquals("SETTINGS_GUARD", (result as SettingsGuardDecision.Block).route)
    }

    @Test
    fun `app info class is blocked`() {
        val result = engine.evaluate(
            packageName = "com.android.settings",
            className = "com.android.settings.applications.InstalledAppDetails",
            config = config
        )
        assertTrue(result is SettingsGuardDecision.Block)
    }

    @Test
    fun `wifi settings class is allowed`() {
        val result = engine.evaluate(
            packageName = "com.android.settings",
            className = "com.android.settings.wifi.WifiSettings",
            config = config
        )
        assertEquals(SettingsGuardDecision.Allow, result)
    }

    @Test
    fun `display settings class is allowed`() {
        val result = engine.evaluate(
            packageName = "com.android.settings",
            className = "com.android.settings.DisplaySettings",
            config = config
        )
        assertEquals(SettingsGuardDecision.Allow, result)
    }

    @Test
    fun `unknown settings class is logged but allowed`() {
        val unknownClass = "com.android.settings.some.UnknownNewScreen"
        val result = engine.evaluate(
            packageName = "com.android.settings",
            className = unknownClass,
            config = config
        )
        assertTrue(result is SettingsGuardDecision.AllowLogged)
        assertEquals(unknownClass, (result as SettingsGuardDecision.AllowLogged).unknownClass)
    }

    @Test
    fun `non-settings package is not affected`() {
        val result = engine.evaluate(
            packageName = "com.instagram.android",
            className = "com.instagram.android.MainActivity",
            config = config
        )
        assertEquals(SettingsGuardDecision.Allow, result)
    }

    @Test
    fun `uninstaller activity class is blocked`() {
        val result = engine.evaluate(
            packageName = "com.android.packageinstaller",
            className = "com.android.packageinstaller.UninstallerActivity",
            config = config
        )
        assertTrue(result is SettingsGuardDecision.Block)
        assertEquals("UNINSTALL_GUARD", (result as SettingsGuardDecision.Block).route)
    }

    @Test
    fun `samsung settings package is treated as settings`() {
        val samsungConfig = config.copy(
            settingsPackages = config.settingsPackages + "com.samsung.android.settings"
        )
        val result = engine.evaluate(
            packageName = "com.samsung.android.settings",
            className = "com.samsung.android.settings.accessibility.AccessibilitySettings",
            config = samsungConfig
        )
        assertTrue(result is SettingsGuardDecision.Block)
    }

    @Test
    fun `emergency override window allows blocked screens temporarily`() {
        val now = 1_000_000L
        engine.grantEmergencyOverride(now)

        assertTrue(engine.isEmergencyOverrideActive(now + 1000L))

        val result = engine.evaluate(
            packageName = "com.android.settings",
            className = "com.android.settings.accessibility.AccessibilitySettings",
            config = config,
            now = now + 1000L
        )
        assertEquals(SettingsGuardDecision.Allow, result)
    }
}
