package com.rick.jetpacksecurityapi37.identity

import android.content.Context
import android.os.Build
import androidx.security.identity.IdentityCredentialStore

class IdentityCredentialLab(private val context: Context) {

    fun inspect(): String = buildString {
        appendLine("Identity Credential stores mobile documents (for example mDL).")
        appendLine("The Jetpack artifact adds a Keystore-backed software path when hardware is missing.")
        appendLine()
        appendLine(inspectJetpack())
        appendLine()
        appendLine(inspectPlatformHardware())
    }

    private fun inspectJetpack(): String = buildString {
        appendLine("Jetpack androidx.security.identity:")
        try {
            val store = IdentityCredentialStore.getInstance(context)
            appendLine("Store class: ${store.javaClass.name}")
            val supported = runCatching { store.supportedDocTypes.toList() }.getOrElse { emptyList() }
            appendLine("Supported doc types: ${supported.ifEmpty { listOf("(none reported)") }}")
            val name = "jetsec-review-cred"
            runCatching { store.deleteCredentialByName(name) }
            val docType = supported.firstOrNull() ?: "org.iso.18013.5.1.mDL"
            val writable = store.createCredential(name, docType)
            val chain = writable.getCredentialKeyCertificateChain("jetsec-challenge".toByteArray())
            appendLine("Created '$name' with docType=$docType")
            appendLine("CredentialKey certs: ${chain.size}")
            runCatching { store.deleteCredentialByName(name) }
        } catch (t: Throwable) {
            appendLine("${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun inspectPlatformHardware(): String = buildString {
        appendLine("Platform android.security.identity (API 30+ hardware path):")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            appendLine("Skipped: device SDK ${Build.VERSION.SDK_INT} < 30.")
            return@buildString
        }
        try {
            val store = android.security.identity.IdentityCredentialStore.getInstance(context)
            if (store == null) {
                appendLine("No hardware-backed IdentityCredentialStore on this device.")
            } else {
                appendLine("Hardware store present: ${store.javaClass.name}")
                appendLine("Supported: ${store.supportedDocTypes.joinToString()}")
            }
        } catch (t: Throwable) {
            appendLine("${t.javaClass.simpleName}: ${t.message}")
        }
    }
}
