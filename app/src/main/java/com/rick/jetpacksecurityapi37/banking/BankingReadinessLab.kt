package com.rick.jetpacksecurityapi37.banking

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import com.rick.jetpacksecurityapi37.policy.PolicyCheck
import com.rick.jetpacksecurityapi37.policy.PolicyReport
import java.io.File
import java.security.KeyStore
import java.util.Locale
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

/**
 * Scores device signals a typical retail banking app uses after install
 * (and that Play uses for certified devices). This cannot answer “will bank X
 * install here?” — country, ABI, that APK’s minSdk, and Play Integrity are private.
 */
class BankingReadinessLab(private val context: Context) {

    fun evaluate(): PolicyReport {
        val checks = listOf(
            checkOsVersion(),
            checkNotEmulator(),
            checkOfficialBuild(),
            checkLockScreen(),
            checkHardwareKeystore(),
            checkPlayStore(),
            checkPlayServices(),
            checkVerifiedBoot(),
            checkBootloader(),
            checkRootHints(),
            checkSecurityPatch(),
            checkStrongBox(),
            checkStrongBiometric(),
            checkAdb(),
        )
        return PolicyReport.from(
            checks = checks,
            allowedSummary = "No blocking failures. A typical retail banking app could likely be installed from Play and reach a login screen. Individual banks still apply their own Play Integrity / country / version rules, which this device-side scan cannot see.",
            blockedPrefix = "Play may still let you download an APK, but most banking apps refuse enroll/login. Blocking failures:",
            allowedTitle = "ALLOWED (typical banking policy)",
            blockedTitle = "NOT ALLOWED (typical banking policy)",
        )
    }

    /**
     * Pass or fail based on SDK min version of typical banks of this writing.
     *
     * @return PolicyCheck
     */
    private fun checkOsVersion(): PolicyCheck {
        val sdk = Build.VERSION.SDK_INT
        val passed = sdk >= TYPICAL_BANK_MIN_SDK
        return PolicyCheck(
            name = "Android version",
            passed = passed,
            blocking = true,
            detail = "SDK $sdk (Android ${Build.VERSION.RELEASE}). Typical 2026 banking apps require API $TYPICAL_BANK_MIN_SDK+.",
        )
    }

    private fun checkNotEmulator(): PolicyCheck {
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
        val product = Build.PRODUCT.lowercase(Locale.US)
        val hardware = Build.HARDWARE.lowercase(Locale.US)
        val model = Build.MODEL.lowercase(Locale.US)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val emulator = fingerprint.contains("generic") ||
            fingerprint.contains("emulator") ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            model.contains("emulator") ||
            manufacturer.contains("genymotion")
        return PolicyCheck(
            name = "Physical device (not emulator)",
            passed = !emulator,
            blocking = true,
            detail = if (emulator) {
                "Looks like an emulator (product=${Build.PRODUCT}, hardware=${Build.HARDWARE}, fingerprint=${Build.FINGERPRINT}). Banks reject these."
            } else {
                "product=${Build.PRODUCT} hardware=${Build.HARDWARE} model=${Build.MODEL}"
            },
        )
    }

    private fun checkOfficialBuild(): PolicyCheck {
        val tags = Build.TAGS.orEmpty()
        val type = Build.TYPE.orEmpty()
        val testKeys = tags.contains("test-keys")
        val userBuild = type == "user"
        val passed = !testKeys && userBuild
        return PolicyCheck(
            name = "User production build",
            passed = passed,
            blocking = true,
            detail = "TYPE=$type TAGS=$tags. Banks want TYPE=user without test-keys (userdebug/eng and unsigned engineering builds fail).",
        )
    }

    private fun checkLockScreen(): PolicyCheck {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val secure = km.isDeviceSecure
        return PolicyCheck(
            name = "Secure lock screen",
            passed = secure,
            blocking = true,
            detail = if (secure) {
                "PIN/pattern/password (or equivalent) is set. Needed for Keystore auth-bound keys and most bank apps."
            } else {
                "No secure lock screen. Banking apps usually refuse to run until a PIN/password is set."
            },
        )
    }

    private fun checkHardwareKeystore(): PolicyCheck {
        return try {
            val key = getOrCreateProbeKey(ALIAS_HW, strongBox = false)
            val info = keyInfo(key)
            val hardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                info.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ||
                    info.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
            } else {
                @Suppress("DEPRECATION")
                info.isInsideSecureHardware
            }
            PolicyCheck(
                name = "Hardware-backed Keystore",
                passed = hardware,
                blocking = true,
                detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "KeyInfo.securityLevel=${info.securityLevel} (TEE=${KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT}, StrongBox=${KeyProperties.SECURITY_LEVEL_STRONGBOX})."
                } else {
                    @Suppress("DEPRECATION")
                    "isInsideSecureHardware=${info.isInsideSecureHardware}"
                },
            )
        } catch (t: Throwable) {
            PolicyCheck(
                name = "Hardware-backed Keystore",
                passed = false,
                blocking = true,
                detail = "Could not create probe key: ${t.javaClass.simpleName}: ${t.message}",
            )
        }
    }

    private fun checkPlayStore(): PolicyCheck {
        val installed = isInstalled("com.android.vending")
        return PolicyCheck(
            name = "Google Play Store present",
            passed = installed,
            blocking = true,
            detail = if (installed) {
                "com.android.vending is installed. Needed to install most banking apps and to pass Play Integrity."
            } else {
                "Play Store missing. Sideload-only devices usually cannot install the official bank APK and fail integrity."
            },
        )
    }

    private fun checkPlayServices(): PolicyCheck {
        val installed = isInstalled("com.google.android.gms")
        return PolicyCheck(
            name = "Google Play services present",
            passed = installed,
            blocking = true,
            detail = if (installed) {
                "com.google.android.gms is installed. Play Integrity (the replacement for SafetyNet) runs here. This lab does not call Integrity because that needs a Cloud project number and a backend to decrypt the verdict."
            } else {
                "Play services missing. Banks that require MEETS_DEVICE_INTEGRITY / MEETS_STRONG_INTEGRITY will refuse."
            },
        )
    }

    private fun checkVerifiedBoot(): PolicyCheck {
        val state = systemProperty("ro.boot.verifiedbootstate").ifBlank { "unknown" }
        val passed = state.equals("green", ignoreCase = true)
        val known = state != "unknown"
        return PolicyCheck(
            name = "Verified Boot",
            passed = if (known) passed else true,
            blocking = known,
            detail = "ro.boot.verifiedbootstate=$state (green=locked official, yellow=self-signed, orange=custom, red=failed). Unknown means the property was hidden from this app.",
        )
    }

    private fun checkBootloader(): PolicyCheck {
        val locked = systemProperty("ro.boot.flash.locked")
        val vbmeta = systemProperty("ro.boot.vbmeta.device_state")
        val passed = locked == "1" || vbmeta.equals("locked", ignoreCase = true)
        val known = locked.isNotBlank() || vbmeta.isNotBlank()
        return PolicyCheck(
            name = "Bootloader locked",
            passed = if (known) passed else true,
            blocking = known,
            detail = "ro.boot.flash.locked=$locked ro.boot.vbmeta.device_state=$vbmeta. Unlocked bootloaders fail Play STRONG integrity and most banks.",
        )
    }

    private fun checkRootHints(): PolicyCheck {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/system/bin/magisk",
            "/sbin/magisk",
        ).filter { File(it).exists() }
        val magisk = isInstalled("com.topjohnwu.magisk")
        val rooted = suPaths.isNotEmpty() || magisk
        return PolicyCheck(
            name = "No obvious root",
            passed = !rooted,
            blocking = true,
            detail = if (rooted) {
                "Root hints: suFiles=$suPaths magiskPackage=$magisk. Hidden Magisk can still evade this check; banks also use Play Integrity."
            } else {
                "No su binary in common paths and Magisk package not visible. This is a heuristic, not a proof of an unrooted device."
            },
        )
    }

    private fun checkSecurityPatch(): PolicyCheck {
        val patch = Build.VERSION.SECURITY_PATCH
        val now = System.currentTimeMillis()
        val ageDays = SecurityPatchAge.ageDays(patch, now)
        val passed = SecurityPatchAge.isFresh(patch, now, PATCH_BLOCKING_DAYS)
        return PolicyCheck(
            name = "Security patch age",
            passed = passed,
            blocking = true,
            detail = "SECURITY_PATCH=$patch (${ageDays ?: "unparsed"} days old). This lab fails patches older than $PATCH_BLOCKING_DAYS days. Banks set their own cutoff; Play Integrity also encodes patch age.",
        )
    }

    private fun checkStrongBox(): PolicyCheck {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return PolicyCheck(
                name = "StrongBox (optional)",
                passed = false,
                blocking = false,
                detail = "SDK ${Build.VERSION.SDK_INT} < 28. StrongBox shipped in Android 9. Optional; does not fail the banking banner.",
            )
        }
        val feature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        val created = if (feature) {
            runCatching {
                val key = getOrCreateProbeKey(ALIAS_SB, strongBox = true)
                val info = keyInfo(key)
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    info.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
            }.getOrDefault(false)
        } else {
            false
        }
        return PolicyCheck(
            name = "StrongBox (optional)",
            passed = created,
            blocking = false,
            detail = "FEATURE_STRONGBOX_KEYSTORE=$feature, key created=$created. Many banks do not require StrongBox. High-assurance / some EU wallets prefer it.",
        )
    }

    private fun checkStrongBiometric(): PolicyCheck {
        val status = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val passed = status == BiometricManager.BIOMETRIC_SUCCESS
        val label = when (status) {
            BiometricManager.BIOMETRIC_SUCCESS -> "BIOMETRIC_SUCCESS"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "NO_HARDWARE"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "HW_UNAVAILABLE"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "NONE_ENROLLED"
            else -> "code $status"
        }
        return PolicyCheck(
            name = "Class 3 biometric enrolled (optional)",
            passed = passed,
            blocking = false,
            detail = "$label. Optional for install. Required later if the bank uses biometric step-up or Identity Credential presentation.",
        )
    }

    private fun checkAdb(): PolicyCheck {
        val adb = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        return PolicyCheck(
            name = "USB debugging off (optional)",
            passed = !adb,
            blocking = false,
            detail = if (adb) {
                "ADB is on. Most banks still install; a few treat developer options as a risk signal."
            } else {
                "ADB disabled."
            },
        )
    }

    private fun isInstalled(packageName: String): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        }.getOrDefault(false)
    }

    private fun systemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val get = clazz.getMethod("get", String::class.java)
            (get.invoke(null, key) as? String).orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun getOrCreateProbeKey(alias: String, strongBox: Boolean): SecretKey {
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
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(builder.build())
        return generator.generateKey()
    }

    private fun keyInfo(key: SecretKey): KeyInfo {
        val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
        return factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS_HW = "jetsec_bank_probe_aes"
        const val ALIAS_SB = "jetsec_bank_probe_strongbox"
        const val TYPICAL_BANK_MIN_SDK = 28
        const val PATCH_BLOCKING_DAYS = 365L
    }
}
