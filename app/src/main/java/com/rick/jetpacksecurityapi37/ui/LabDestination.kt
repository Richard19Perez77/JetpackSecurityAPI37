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
    KEYSTORE(
        "keystore",
        "Android Keystore",
        "Hardware-backed keys and StrongBox",
    ),
    AUTHENTICATOR(
        "authenticator",
        "App Authenticator",
        "Verify another app by package name + cert digest",
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
    BIOMETRIC(
        "biometric",
        "Biometric-gated key",
        "Keystore key that requires BIOMETRIC_STRONG",
    ),
    CREDENTIALS(
        "credentials",
        "Credential Manager",
        "Platform neighbor for passwords and passkeys",
    ),
    BANKING(
        "banking",
        "Banking app readiness",
        "Device signals banks use to allow install and login",
    ),
    BACKUP(
        "backup",
        "Backup exclusion",
        "Why encrypted files must stay out of Auto Backup",
    ),
}
