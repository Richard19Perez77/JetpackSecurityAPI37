package com.rick.jetpacksecurityapi37.capture

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.rick.jetpacksecurityapi37.policy.PolicyCheck
import com.rick.jetpacksecurityapi37.policy.PolicyReport

/**
 * Scores whether this window looks like a bank / password-manager screen
 * that should refuse screenshots and screen share.
 *
 * [android.view.WindowManager.LayoutParams.FLAG_SECURE] is set by *this* app
 * on this lab screen. The lab cannot see whether Chase or a password manager
 * set the flag in *their* APK.
 */
class ScreenCaptureLab(private val activity: Activity) {

    fun applyWindowGuards() {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun clearWindowGuards() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun evaluate(): PolicyReport {
        applyWindowGuards()
        val checks = listOf(
            checkFlagSecure(),
            checkNotPictureInPicture(),
            checkNotMultiWindow(),
            checkNoExtraDisplays(),
            checkCaptureCallbackApi(),
        )
        return PolicyReport.from(
            checks = checks,
            allowedSummary = "This window is FLAG_SECURE and is not in PiP. Banks and password managers still cannot prove a kernel-level recorder is absent; FLAG_SECURE is the usual app-side control.",
            blockedPrefix = "Typical login/transfer screens would treat this as screenshot-exposed. Blocking failures:",
            allowedTitle = "PROTECTED (typical bank/password-manager screen)",
            blockedTitle = "EXPOSED (typical bank/password-manager screen)",
        )
    }

    private fun checkFlagSecure(): PolicyCheck {
        val secure = activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        return PolicyCheck(
            name = "FLAG_SECURE on this window",
            passed = secure,
            blocking = true,
            detail = if (secure) {
                "WindowManager.LayoutParams.FLAG_SECURE is set. The system should block screenshots, recents thumbnails, and most screen-share of this window."
            } else {
                "FLAG_SECURE is off. A screenshot or recents thumbnail can capture whatever is on this screen."
            },
        )
    }

    private fun checkNotPictureInPicture(): PolicyCheck {
        val pip = activity.isInPictureInPictureMode
        return PolicyCheck(
            name = "Not in picture-in-picture",
            passed = !pip,
            blocking = true,
            detail = if (pip) {
                "PiP can show a scaled copy of the task. Login screens usually refuse PiP."
            } else {
                "Activity.isInPictureInPictureMode is false."
            },
        )
    }

    private fun checkNotMultiWindow(): PolicyCheck {
        val split = activity.isInMultiWindowMode
        return PolicyCheck(
            name = "Not in split-screen (optional)",
            passed = !split,
            blocking = false,
            detail = if (split) {
                "Activity.isInMultiWindowMode is true. Some banks block split-screen so another app cannot sit beside a transfer UI."
            } else {
                "Single-window. Optional because many banks still allow split-screen."
            },
        )
    }

    private fun checkNoExtraDisplays(): PolicyCheck {
        val manager = activity.getSystemService(DisplayManager::class.java)
        val extras = manager.displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
        return PolicyCheck(
            name = "No extra / virtual displays (optional)",
            passed = extras.isEmpty(),
            blocking = false,
            detail = if (extras.isEmpty()) {
                "Only Display.DEFAULT_DISPLAY is present."
            } else {
                "Extra displays: ${extras.joinToString { "${it.name} id=${it.displayId}" }}. Cast, desktop mode, emulators, or screen share can create these. Not blocking because emulators often have extras."
            },
        )
    }

    private fun checkCaptureCallbackApi(): PolicyCheck {
        val available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        return PolicyCheck(
            name = "Screen-capture callback API (optional)",
            passed = available,
            blocking = false,
            detail = if (available) {
                "API ${Build.VERSION.SDK_INT} has Activity.registerScreenCaptureCallback. This lab does not keep a live callback; FLAG_SECURE is the synchronous control."
            } else {
                "API ${Build.VERSION.SDK_INT} < 34. registerScreenCaptureCallback is unavailable. FLAG_SECURE still applies."
            },
        )
    }
}
