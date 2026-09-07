package com.rick.jetpacksecurityapi37.keystore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

class KeystoreInfoLab(private val context: Context) {

    fun inspect(): String = buildString {
        appendLine("AndroidKeyStore is the platform neighbor under Jetpack Security.")
        appendLine("Keys can be hardware-backed. StrongBox is a separate tamper-resistant chip, when present.")
        appendLine()
        appendLine("FEATURE_STRONGBOX_KEYSTORE: ${context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)}")
        appendLine("SDK: ${Build.VERSION.SDK_INT}")
        appendLine()
        appendLine(describeKey(ALIAS_SOFTWARE, requestStrongBox = false))
        appendLine(describeKey(ALIAS_STRONGBOX, requestStrongBox = true))
    }

    private fun describeKey(alias: String, requestStrongBox: Boolean): String {
        return try {
            val key = getOrCreate(alias, requestStrongBox)
            val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
            val info = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
            buildString {
                appendLine("alias=$alias requestStrongBox=$requestStrongBox")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    appendLine("  securityLevel=${info.securityLevel}")
                    appendLine(
                        "  strongBox=${info.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX}",
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appendLine("  insideSecureHardware=${info.isInsideSecureHardware}")
                }
            }
        } catch (t: Throwable) {
            "alias=$alias failed: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    private fun getOrCreate(alias: String, requestStrongBox: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
        if (requestStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(builder.build())
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS_SOFTWARE = "jetsec_info_aes"
        const val ALIAS_STRONGBOX = "jetsec_info_aes_strongbox"
    }
}
