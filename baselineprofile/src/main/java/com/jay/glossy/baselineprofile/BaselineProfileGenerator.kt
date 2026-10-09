package com.jay.glossy.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupAndHomeScrolling() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), UI_TIMEOUT_MS)
        device.waitForIdle()

        // Exercise the first screen and its scrolling path without relying on
        // network-backed content being present on the benchmark device.
        repeat(2) {
            val width = device.displayWidth
            val height = device.displayHeight
            device.swipe(width / 2, height * 3 / 4, width / 2, height / 3, 12)
            device.waitForIdle()
        }
    }

    @Test
    fun searchAndLibraryNavigation() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = false,
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), UI_TIMEOUT_MS)
        device.waitForIdle()

        // The app may use either an accessibility description or visible text
        // for these navigation destinations; skip a tap if the item is absent.
        val openedSearch = clickIfPresent(device, By.desc("Search"))
            || clickIfPresent(device, By.text("Search"))
        device.waitForIdle()
        if (openedSearch) {
            device.pressBack()
            device.waitForIdle()
        }

        clickIfPresent(device, By.desc("Library"))
            || clickIfPresent(device, By.text("Library"))
        device.waitForIdle()

        repeat(1) {
            val width = device.displayWidth
            val height = device.displayHeight
            device.swipe(width / 2, height * 3 / 4, width / 2, height / 3, 10)
            device.waitForIdle()
        }

        // When the home content exposes a Play All action, include the player
        // and lyrics screens too. These are optional so empty/offline CI devices
        // can still produce a valid profile without relying on remote music data.
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
        val playAll = device.findObject(By.desc("Play All"))
            ?: device.findObject(By.text("Play All"))
        if (playAll != null) {
            playAll.click()
            device.waitForIdle()
            val lyrics = device.findObject(By.desc("Lyrics"))
            if (lyrics != null) {
                lyrics.click()
                device.waitForIdle()
                device.pressBack()
                device.waitForIdle()
            }
        }
    }

    private fun clickIfPresent(device: UiDevice, selector: androidx.test.uiautomator.BySelector): Boolean {
        val item = device.findObject(selector) ?: return false
        item.click()
        return true
    }

    companion object {
        private const val TARGET_PACKAGE = "com.jay.glossy"
        private const val UI_TIMEOUT_MS = 10_000L
    }
}
