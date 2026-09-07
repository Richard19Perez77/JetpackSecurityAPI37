package com.rick.jetpacksecurityapi37.crypto

enum class CryptoEra(val title: String, val summary: String) {
    LEGACY_JETSEC(
        title = "Previous: security-crypto",
        summary = "MasterKey + EncryptedSharedPreferences / EncryptedFile (deprecated in 1.1.0)",
    ),
    TINK_DATASTORE(
        title = "Current: Tink + DataStore",
        summary = "Keystore-wrapped Tink keyset, AEAD values, async DataStore",
    ),
    KEYSTORE_DIRECT(
        title = "Neighbor: Android Keystore",
        summary = "AES-GCM with KeyGenerator in AndroidKeyStore",
    ),
}
