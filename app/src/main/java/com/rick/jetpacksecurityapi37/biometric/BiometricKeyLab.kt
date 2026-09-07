package com.rick.jetpacksecurityapi37.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class BiometricKeyLab(private val context: Context) {

    fun availability(): String {
        val manager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val status = manager.canAuthenticate(authenticators)
        val statusLabel = when (status) {
            BiometricManager.BIOMETRIC_SUCCESS -> "BIOMETRIC_SUCCESS"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "BIOMETRIC_ERROR_NO_HARDWARE"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "BIOMETRIC_ERROR_HW_UNAVAILABLE"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "BIOMETRIC_ERROR_NONE_ENROLLED"
            else -> "code $status"
        }
        return "BiometricManager (BIOMETRIC_STRONG): $statusLabel"
    }

    suspend fun encryptSample(activity: FragmentActivity): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val unlocked = awaitPrompt(activity, BiometricPrompt.CryptoObject(cipher))
            val encrypted = unlocked.doFinal(SAMPLE.toByteArray())
            "Encrypted after BIOMETRIC_STRONG unlocked this Cipher. bytes=${encrypted.size}"
        } catch (t: KeyPermanentlyInvalidatedException) {
            "Key invalidated after biometric change: ${t.message}"
        } catch (t: Throwable) {
            "${t.javaClass.simpleName}: ${t.message}"
        }
    }

    private suspend fun awaitPrompt(
        activity: FragmentActivity,
        crypto: BiometricPrompt.CryptoObject,
    ): Cipher = suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (!cont.isActive) return
                    val unlocked = result.cryptoObject?.cipher
                    if (unlocked != null) {
                        cont.resume(unlocked)
                    } else {
                        cont.resumeWith(Result.failure(IllegalStateException("No cipher on result")))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!cont.isActive) return
                    cont.resumeWith(Result.failure(IllegalStateException("($errorCode) $errString")))
                }

                override fun onAuthenticationFailed() = Unit
            },
        )
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Jetpack Security key")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build(),
            crypto,
        )
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(builder.build())
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "jetsec_biometric_aes_per_use"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SAMPLE = "biometric-secret"
    }
}
