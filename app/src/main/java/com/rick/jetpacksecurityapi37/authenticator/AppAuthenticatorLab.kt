package com.rick.jetpacksecurityapi37.authenticator

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.security.app.authenticator.AppAuthenticator
import com.rick.jetpacksecurityapi37.R
import java.io.ByteArrayInputStream
import java.security.MessageDigest

class AppAuthenticatorLab(private val context: Context) {

    fun inspect(): String = buildString {
        val digest = signingCertSha256Hex()
        appendLine("This app package: ${context.packageName}")
        appendLine("SHA-256 cert digest (hex):")
        appendLine(digest ?: "(unavailable)")
        appendLine()
        appendLine("Static XML (${context.resources.getResourceEntryName(R.xml.app_authenticator)}) uses a placeholder digest, so a production-style check of this debug build usually returns NO_MATCH.")
        appendLine()
        appendLine(checkAgainstStaticXml())
        appendLine()
        appendLine(checkAgainstGeneratedXml(expectedMatch = true))
        appendLine(checkAgainstGeneratedXml(expectedMatch = false))
    }

    private fun checkAgainstStaticXml(): String {
        return try {
            val authenticator = AppAuthenticator.createFromResource(context, R.xml.app_authenticator)
            val result = authenticator.checkAppIdentity(context.packageName)
            "Static XML checkAppIdentity: ${label(result)}"
        } catch (t: Throwable) {
            "Static XML check failed: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    private fun checkAgainstGeneratedXml(expectedMatch: Boolean): String {
        val realDigest = signingCertSha256Hex() ?: return "Cannot build runtime XML without a digest."
        val digestInXml = if (expectedMatch) realDigest else "00".repeat(32)
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <app-authenticator>
                <expected-identity>
                    <package name="${context.packageName}">
                        <cert-digest>$digestInXml</cert-digest>
                    </package>
                </expected-identity>
            </app-authenticator>
        """.trimIndent()
        return try {
            val authenticator = AppAuthenticator.createFromInputStream(
                context,
                ByteArrayInputStream(xml.toByteArray()),
            )
            val result = authenticator.checkAppIdentity(context.packageName)
            val expected = if (expectedMatch) "expect MATCH" else "expect NO_MATCH"
            "Runtime XML ($expected): ${label(result)}"
        } catch (t: Throwable) {
            "Runtime XML check failed: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    private fun signingCertSha256Hex(): String? {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES,
                ).signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return null
            val digest = MessageDigest.getInstance("SHA-256").digest(cert)
            digest.joinToString("") { byte -> "%02x".format(byte) }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not compute signing digest", t)
            null
        }
    }

    private fun label(result: Int): String = when (result) {
        AppAuthenticator.SIGNATURE_MATCH -> "SIGNATURE_MATCH"
        AppAuthenticator.SIGNATURE_NO_MATCH -> "SIGNATURE_NO_MATCH"
        else -> "code $result"
    }

    private companion object {
        const val TAG = "AppAuthenticatorLab"
    }
}
