package com.rick.jetpacksecurityapi37.overlay

import android.app.Activity
import android.os.Build
import android.provider.Settings
import android.view.View
import com.rick.jetpacksecurityapi37.policy.PolicyCheck
import com.rick.jetpacksecurityapi37.policy.PolicyReport

/**
 * Scores tapjacking / overlay defenses a login screen usually wants.
 *
 * Cannot list every app with SYSTEM_ALERT_WINDOW without QUERY_ALL_PACKAGES.
 * Checks *this* window, *this* app's overlay permission, and enabled
 * accessibility services from [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES].
 */
class OverlayTapjackingLab(private val activity: Activity) {

    fun applyWindowGuards() {
        activity.window.decorView.filterTouchesWhenObscured = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { activity.window.setHideOverlayWindows(true) }
        }
    }

    fun clearWindowGuards() {
        activity.window.decorView.filterTouchesWhenObscured = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { activity.window.setHideOverlayWindows(false) }
        }
    }

    fun evaluate(): PolicyReport {
        applyWindowGuards()
        val checks = listOf(
            checkFilterTouches(),
            checkHideOverlayWindows(),
            checkThisAppCannotOverlay(),
            checkAccessibilityServices(),
        )
        return PolicyReport.from(
            checks = checks,
            allowedSummary = "This window drops touches when another window covers it, and this app cannot draw overlays. Hidden Magisk/overlay malware can still evade this. Accessibility is optional so TalkBack users are not locked out.",
            blockedPrefix = "A typical login screen would treat this as tapjacking-exposed. Blocking failures:",
            allowedTitle = "HARDENED (typical login overlay policy)",
            blockedTitle = "EXPOSED (typical login overlay policy)",
        )
    }

    private fun checkFilterTouches(): PolicyCheck {
        val view: View = activity.window.decorView
        val filtered = view.filterTouchesWhenObscured
        return PolicyCheck(
            name = "filterTouchesWhenObscured",
            passed = filtered,
            blocking = true,
            detail = if (filtered) {
                "View.filterTouchesWhenObscured is true on the decor view. Touches are ignored when another window obscures this one (MotionEvent.FLAG_WINDOW_IS_OBSCURED)."
            } else {
                "filterTouchesWhenObscured is false. A fake Confirm button drawn by an overlay could steal taps."
            },
        )
    }

    private fun checkHideOverlayWindows(): PolicyCheck {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return PolicyCheck(
                name = "Hide overlay windows",
                passed = false,
                blocking = false,
                detail = "SDK ${Build.VERSION.SDK_INT} < 31. Window.setHideOverlayWindows arrived in Android 12. Optional on older devices.",
            )
        }
        val hidden = runCatching {
            activity.window.setHideOverlayWindows(true)
            true
        }.getOrDefault(false)
        return PolicyCheck(
            name = "Hide overlay windows",
            passed = hidden,
            blocking = true,
            detail = if (hidden) {
                "Window.setHideOverlayWindows(true) succeeded (needs android.permission.HIDE_OVERLAY_WINDOWS). TYPE_APPLICATION_OVERLAY windows should not cover this activity."
            } else {
                "setHideOverlayWindows failed. Add HIDE_OVERLAY_WINDOWS in the manifest or the platform refused the call."
            },
        )
    }

    private fun checkThisAppCannotOverlay(): PolicyCheck {
        val can = Settings.canDrawOverlays(activity)
        return PolicyCheck(
            name = "This app cannot draw overlays",
            passed = !can,
            blocking = true,
            detail = if (can) {
                "Settings.canDrawOverlays is true for ${activity.packageName}. Banks and password managers almost never need SYSTEM_ALERT_WINDOW on their own package."
            } else {
                "This package does not have the Draw over other apps permission."
            },
        )
    }

    private fun checkAccessibilityServices(): PolicyCheck {
        val enabled = Settings.Secure.getString(
            activity.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val none = enabled.isBlank()
        return PolicyCheck(
            name = "No accessibility services (optional)",
            passed = none,
            blocking = false,
            detail = if (none) {
                "Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES is empty. Optional because TalkBack is legitimate; malware can also abuse accessibility."
            } else {
                "Enabled: $enabled. Some banks warn or block; this lab does not fail the banner so assistive tech still works."
            },
        )
    }
}
