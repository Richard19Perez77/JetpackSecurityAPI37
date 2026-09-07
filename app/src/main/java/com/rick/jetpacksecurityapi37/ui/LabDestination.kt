package com.rick.jetpacksecurityapi37.ui

enum class LabDestination(
    val route: String,
    val title: String,
    val subtitle: String,
) {
    STORAGE(
        "storage",
        "Encrypted preferences",
        "Switch EncryptedSharedPreferences, Tink+DataStore, or Keystore AES-GCM",
    ),
    FILES(
        "files",
        "Encrypted files",
        "EncryptedFile vs Tink StreamingAead vs Keystore",
    ),
    MIGRATION(
        "migration",
        "Crypto migration",
        "Copy a legacy EncryptedSharedPreferences value into Tink+DataStore",
    ),
    BACKUP(
        "backup",
        "Backup exclusion",
        "Why encrypted files must stay out of Auto Backup",
    ),
    AUTHENTICATOR(
        "authenticator",
        "App Authenticator",
        "Verify another app by package name + cert digest",
    ),
    KEYSTORE(
        "keystore",
        "Android Keystore",
        "Hardware-backed keys and StrongBox",
    ),
    BIOMETRIC(
        "biometric",
        "Biometric-gated key",
        "Keystore key that requires BIOMETRIC_STRONG",
    ),
    BANKING(
        "banking",
        "Banking app readiness",
        "Device signals banks use to allow install and login",
    ),
    SCREEN_CAPTURE(
        "screen-capture",
        "Screenshot / FLAG_SECURE",
        "Block screenshots on login-style screens",
    ),
    OVERLAY(
        "overlay",
        "Overlay / tapjacking",
        "Drop obscured touches and hide overlay windows",
    ),
    INSTALL_SOURCE(
        "install-source",
        "Installer source",
        "Play Store vs sideload vs debug install",
    ),
    PAYMENT(
        "payment",
        "Contactless payment (NFC)",
        "NFC + HCE + lock screen floor for tap-to-pay",
    ),
    IDENTITY(
        "identity",
        "Identity Credential",
        "Jetpack software store vs platform hardware store",
    ),
    STATE(
        "state",
        "Security State",
        "Live SPL bundle plus a scripted OEM update sample",
    ),
    CREDENTIALS(
        "credentials",
        "Credential Manager",
        "Platform neighbor for passwords and passkeys",
    ),
    ;

    companion object {
        const val CATALOG_ROUTE = "catalog"
    }
}
