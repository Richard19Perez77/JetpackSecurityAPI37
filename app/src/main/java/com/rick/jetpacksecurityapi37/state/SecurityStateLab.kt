package com.rick.jetpacksecurityapi37.state

import android.content.Context
import android.os.Build
import androidx.security.state.SecurityStateManagerCompat

/**
 * Reads the live [SecurityStateManagerCompat] bundle and prints a scripted OEM
 * [MockUpdateInfo] sample so reviewers can see the UpdateInfo shape without a
 * privileged provider.
 */
class SecurityStateLab(private val context: Context) {

    fun inspect(): String = buildString {
        appendLine("compile/target SDK: 37")
        appendLine("Device SDK: ${Build.VERSION.SDK_INT}")
        appendLine("Build.VERSION.SECURITY_PATCH: ${Build.VERSION.SECURITY_PATCH}")
        appendLine()
        appendLine("Live SecurityStateManagerCompat bundle:")
        appendLine(readLiveBundle())
        appendLine()
        appendLine("Scripted OEM provider sample (not from this device):")
        appendLine(mockOemProviderReport())
    }

    private fun readLiveBundle(): String {
        return try {
            val manager = SecurityStateManagerCompat(context)
            val bundle = manager.getGlobalSecurityState()
            if (bundle.isEmpty) {
                "(empty bundle — common on emulators and devices without the SDK 35+ service)"
            } else {
                buildString {
                    for (key in bundle.keySet()) {
                        appendLine("$key=${bundle.get(key)}")
                    }
                }.trim()
            }
        } catch (t: Throwable) {
            "${t.javaClass.simpleName}: ${t.message}"
        }
    }

    private fun mockOemProviderReport(): String {
        val samples = listOf(
            MockUpdateInfo(
                componentName = "system",
                securityPatchLevel = "2026-09-01",
                publishedDate = "2026-09-01",
                uri = "content://oem.updater/system",
            ),
            MockUpdateInfo(
                componentName = "vendor",
                securityPatchLevel = "2026-08-05",
                publishedDate = "2026-08-05",
                uri = "content://oem.updater/vendor",
            ),
            MockUpdateInfo(
                componentName = "kernel",
                securityPatchLevel = "6.1.lts-demo",
                publishedDate = "2026-07-01",
                uri = "content://oem.updater/kernel",
            ),
        )
        return samples.joinToString("\n") {
            "${it.componentName}: SPL=${it.securityPatchLevel} published=${it.publishedDate} uri=${it.uri}"
        } + "\n\nsecurity-state-provider is aimed at OEM/OTA clients with privileged identity checks. Apps usually consume SecurityStateManagerCompat instead of hosting UpdateInfoService."
    }
}

data class MockUpdateInfo(
    val componentName: String,
    val securityPatchLevel: String,
    val publishedDate: String,
    val uri: String,
)
