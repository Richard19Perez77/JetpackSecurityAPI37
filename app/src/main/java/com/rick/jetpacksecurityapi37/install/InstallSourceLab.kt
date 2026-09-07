package com.rick.jetpacksecurityapi37.install

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.rick.jetpacksecurityapi37.policy.PolicyCheck
import com.rick.jetpacksecurityapi37.policy.PolicyReport

/**
 * Scores how *this* APK arrived on the device. Debug installs from Android Studio
 * fail on purpose. A Play-installed release build is what stores expect.
 */
class InstallSourceLab(private val context: Context) {

    fun evaluate(): PolicyReport {
        val checks = listOf(
            checkNotDebuggable(),
            checkInstallerIsPlay(),
            checkPackageSource(),
            checkInitiatingPackage(),
        )
        return PolicyReport.from(
            checks = checks,
            allowedSummary = "This APK looks like a Play Store, non-debug install. That is what most banks require. It is not Play Integrity and cannot see the store listing country.",
            blockedPrefix = "A store-only banking app would reject this install path. Blocking failures:",
            allowedTitle = "STORE INSTALL (typical Play-only policy)",
            blockedTitle = "SIDELOAD / DEBUG (typical Play-only policy)",
        )
    }

    private fun checkNotDebuggable(): PolicyCheck {
        val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return PolicyCheck(
            name = "Not a debuggable APK",
            passed = !debug,
            blocking = true,
            detail = if (debug) {
                "ApplicationInfo.FLAG_DEBUGGABLE is set. Android Studio debug builds fail this on purpose. Banks ship release, non-debuggable APKs."
            } else {
                "APK is not debuggable."
            },
        )
    }

    private fun checkInstallerIsPlay(): PolicyCheck {
        val installer = installerPackage()
        val passed = installer == PLAY_STORE
        return PolicyCheck(
            name = "Installed by Play Store",
            passed = passed,
            blocking = true,
            detail = "installingPackageName=$installer (Play is $PLAY_STORE). Android Studio / adb usually show com.android.shell or a package installer. Sideload is not the same as a Play Integrity token.",
        )
    }

    private fun checkPackageSource(): PolicyCheck {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PolicyCheck(
                name = "Package source is STORE (optional)",
                passed = true,
                blocking = false,
                detail = "SDK ${Build.VERSION.SDK_INT} < 33. PackageManager.InstallSourceInfo.packageSource is API 33+.",
            )
        }
        val source = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).packageSource
        }.getOrNull()
        val passed = source == SOURCE_STORE
        val label = when (source) {
            SOURCE_STORE -> "PACKAGE_SOURCE_STORE"
            SOURCE_LOCAL_FILE -> "PACKAGE_SOURCE_LOCAL_FILE"
            SOURCE_DOWNLOADED_FILE -> "PACKAGE_SOURCE_DOWNLOADED_FILE"
            SOURCE_OTHER -> "PACKAGE_SOURCE_OTHER"
            SOURCE_UNSPECIFIED -> "PACKAGE_SOURCE_UNSPECIFIED"
            null -> "unavailable"
            else -> "code $source"
        }
        return PolicyCheck(
            name = "Package source is STORE",
            passed = passed,
            blocking = true,
            detail = "InstallSourceInfo.packageSource=$label. LOCAL_FILE / DOWNLOADED_FILE is the usual sideload path.",
        )
    }

    private fun checkInitiatingPackage(): PolicyCheck {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return PolicyCheck(
                name = "Initiating package (optional)",
                passed = true,
                blocking = false,
                detail = "SDK ${Build.VERSION.SDK_INT} < 30. initiatingPackageName needs getInstallSourceInfo.",
            )
        }
        val initiating = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).initiatingPackageName
        }.getOrNull()
        val isPlay = initiating == PLAY_STORE
        return PolicyCheck(
            name = "Initiating package is Play (optional)",
            passed = isPlay,
            blocking = false,
            detail = "initiatingPackageName=$initiating. Optional because some OEM update flows initiate from a package other than Play.",
        )
    }

    private fun installerPackage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            }.getOrNull().orEmpty().ifBlank { "(none)" }
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName).orEmpty().ifBlank { "(none)" }
        }
    }

    private companion object {
        const val PLAY_STORE = "com.android.vending"
        const val SOURCE_UNSPECIFIED = 0
        const val SOURCE_OTHER = 1
        const val SOURCE_STORE = 2
        const val SOURCE_LOCAL_FILE = 3
        const val SOURCE_DOWNLOADED_FILE = 4
    }
}
