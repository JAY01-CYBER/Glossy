package com.jay.glossy.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises stable, user-visible Glossy journeys to collect a real profile.
 *
 * This intentionally avoids asserting remote catalogue contents: profile collection
 * must still work when the emulator has no account, saved library, or reliable network.
 * Conditional destinations are visited only when their controls are present.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), UI_TIMEOUT_MS)
        device.waitForIdle(IDLE_TIMEOUT_MS)
        completeFirstRunIfShown(device)
        device.clickIfPresent("Home")
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    @Test
    fun primaryUserJourneys() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
    ) {
        pressHome()
        startActivityAndWait()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), UI_TIMEOUT_MS)
        device.waitForIdle(IDLE_TIMEOUT_MS)

        completeFirstRunIfShown(device)
        exerciseHome(device)
        exercisePrimaryNavigation(device)
        exerciseSearch(device)
        exerciseLibraryCollections(device)
        exerciseSettingsIfAvailable(device)
    }

    private fun completeFirstRunIfShown(device: UiDevice) {
        // A clean emulator can show the guest onboarding. Handle it without requiring
        // a Google account or any external service.
        if (device.clickIfPresent("Continue as guest")) {
            device.waitForIdle(IDLE_TIMEOUT_MS)
            device.findObject(By.clazz("android.widget.EditText"))?.let { field ->
                field.setText("Glossy Profile")
            }
            device.clickIfPresent("Start listening")
            device.waitForIdle(IDLE_TIMEOUT_MS)
        }

        // The community step is optional for existing installs; Next is safe when shown.
        repeat(2) {
            if (device.clickIfPresent("Next")) {
                device.waitForIdle(IDLE_TIMEOUT_MS)
            }
        }
    }

    private fun exerciseHome(device: UiDevice) {
        device.clickIfPresent("Home")
        device.waitForIdle(IDLE_TIMEOUT_MS)
        repeat(3) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 3) / 4,
                device.displayWidth / 2,
                device.displayHeight / 4,
                12,
            )
            device.waitForIdle(500)
        }
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight / 4,
            device.displayWidth / 2,
            (device.displayHeight * 3) / 4,
            12,
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    private fun exercisePrimaryNavigation(device: UiDevice) {
        listOf("Mix", "Library", "Home").forEach { destination ->
            device.clickIfPresent(destination)
            device.waitForIdle(IDLE_TIMEOUT_MS)
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 3) / 4,
                device.displayWidth / 2,
                device.displayHeight / 3,
                10,
            )
            device.waitForIdle(500)
        }
    }

    private fun exerciseSearch(device: UiDevice) {
        if (!device.clickIfPresent("Search")) return
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Compose text fields are exposed as editable accessibility nodes on Android.
        // Do not require a result list: search providers need network access.
        val searchField: UiObject2? = device.findObject(By.clazz("android.widget.EditText"))
        searchField?.let {
            it.click()
            it.setText("glossy")
            device.waitForIdle(500)
            device.pressBack()
            device.waitForIdle(IDLE_TIMEOUT_MS)
        }
    }

    private fun exerciseLibraryCollections(device: UiDevice) {
        if (!device.clickIfPresent("Library")) return
        device.waitForIdle(IDLE_TIMEOUT_MS)

        listOf("History", "Stats", "Liked", "Downloaded").forEach { destination ->
            if (device.clickIfPresent(destination)) {
                device.waitForIdle(IDLE_TIMEOUT_MS)
                device.pressBack()
                device.waitForIdle(IDLE_TIMEOUT_MS)
            }
        }

        device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * 3) / 4,
            device.displayWidth / 2,
            device.displayHeight / 3,
            10,
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    private fun exerciseSettingsIfAvailable(device: UiDevice) {
        // Settings is opened through the app bar in some layouts. Skip it when the
        // current device layout does not expose a Settings accessibility node.
        if (device.clickIfPresent("Settings")) {
            device.waitForIdle(IDLE_TIMEOUT_MS)
            listOf("Appearance", "Player", "Privacy", "Storage").forEach { section ->
                if (device.clickIfPresent(section)) {
                    device.waitForIdle(500)
                    device.pressBack()
                    device.waitForIdle(500)
                }
            }
            device.pressBack()
            device.waitForIdle(IDLE_TIMEOUT_MS)
        }
    }

    private fun UiDevice.clickIfPresent(label: String): Boolean {
        val selectors = listOf(By.desc(label), By.text(label))
        for (selector in selectors) {
            val target = findObject(selector)
            if (target != null && target.isEnabled) {
                target.click()
                return true
            }
        }
        return false
    }

    companion object {
        private const val PACKAGE_NAME = "com.jay.glossy"
        private const val UI_TIMEOUT_MS = 10_000L
        private const val IDLE_TIMEOUT_MS = 2_000L
    }
}
