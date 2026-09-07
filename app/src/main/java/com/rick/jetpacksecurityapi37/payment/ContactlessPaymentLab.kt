package com.rick.jetpacksecurityapi37.payment

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.provider.Settings
import com.rick.jetpacksecurityapi37.policy.PolicyCheck
import com.rick.jetpacksecurityapi37.policy.PolicyReport

/**
 * Scores on-device NFC / HCE signals a wallet or contactless-payment screen
 * cares about. Does not talk to a card network or Play wallet backend.
 */
class ContactlessPaymentLab(private val context: Context) {

    fun evaluate(): PolicyReport {
        val checks = listOf(
            checkNfcFeature(),
            checkNfcEnabled(),
            checkHceFeature(),
            checkLockScreen(),
            checkDefaultPaymentApp(),
        )
        return PolicyReport.from(
            checks = checks,
            allowedSummary = "NFC + HCE are present, NFC is on, and a secure lock screen is set. That is the usual floor for Android host-card-emulation payments. A real wallet still needs its own AID registration and issuer backend.",
            blockedPrefix = "A typical contactless-payment screen would stop here. Blocking failures:",
            allowedTitle = "READY (typical HCE payment floor)",
            blockedTitle = "NOT READY (typical HCE payment floor)",
        )
    }

    /**
     *  Pass or fail based on device NFC hardware.
     *
     *  FEATURE_NFC
     *      System level check for feature flag
     *      hardware support for NFC near field communication
     *
     * @return true if device has FEATURE_NFC
     */
    private fun checkNfcFeature(): PolicyCheck {
        val has = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
        return PolicyCheck(
            name = "NFC hardware",
            passed = has,
            blocking = true,
            detail = if (has) {
                "PackageManager.FEATURE_NFC is present."
            } else {
                "No FEATURE_NFC. Emulators and Wi-Fi-only tablets fail this. Tap-to-pay is not possible."
            },
        )
    }

    /**
     *  Pass or fail based on NFC radio being enabled.
     *      User preference, can change while app is running.
     *
     * @return true if device has NFC radio and is on
     */
    private fun checkNfcEnabled(): PolicyCheck {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        val enabled = adapter?.isEnabled == true
        return PolicyCheck(
            name = "NFC radio on",
            passed = enabled,
            blocking = true,
            detail = when {
                adapter == null -> "NfcAdapter.getDefaultAdapter returned null."
                enabled -> "NfcAdapter.isEnabled is true."
                else -> "NFC hardware exists but is toggled off in Settings."
            },
        )
    }

    /**
     *  Pass or fail based on device HCE hardware.
     *      Critical for payment apps.
     *
     * @return true if device has FEATURE_NFC_HOST_CARD_EMULATION
     */
    private fun checkHceFeature(): PolicyCheck {
        val has = context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_NFC_HOST_CARD_EMULATION,
        )
        return PolicyCheck(
            name = "Host card emulation",
            passed = has,
            blocking = true,
            detail = if (has) {
                "FEATURE_NFC_HOST_CARD_EMULATION is present. Google Wallet-style apps emulate a card in software with a Keystore-backed key."
            } else {
                "No HCE feature. Reader/writer NFC alone is not enough for tap-to-pay."
            },
        )
    }

    /**
     *  Pass or fail based on device lock screen.
     *      Hard requirement for HCE payments on Android.
     *      Payment tokens are stored in secure hardware.
     *      PIN/Patterna and Password NOT Biometrics
     *
     * @return true based on isDeviceSecure flag
     */
    private fun checkLockScreen(): PolicyCheck {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val secure = km.isDeviceSecure
        return PolicyCheck(
            name = "Secure lock screen",
            passed = secure,
            blocking = true,
            detail = if (secure) {
                "KeyguardManager.isDeviceSecure is true. Android payment apps require this before HCE can run."
            } else {
                "No PIN/pattern/password. Contactless payment is disabled until a lock screen exists."
            },
        )
    }

    /**
     *  Pass or fail based on default payment app.
     *      Use secure settings
     *      Read current default HCE host card emulation payment application
     *
     * @return  true if device has a default payment app set
     */
    private fun checkDefaultPaymentApp(): PolicyCheck {
        val component = Settings.Secure.getString(
            context.contentResolver,
            "nfc_payment_default_component",
        )
        return PolicyCheck(
            name = "Default payment app (optional)",
            passed = !component.isNullOrBlank(),
            blocking = false,
            detail = if (!component.isNullOrBlank()) {
                "Settings.Secure nfc_payment_default_component=$component. Optional because this catalog is not that wallet."
            } else {
                "No default HCE payment component. Users set this in Settings → Connected devices → NFC → Contactless payments."
            },
        )
    }
}
