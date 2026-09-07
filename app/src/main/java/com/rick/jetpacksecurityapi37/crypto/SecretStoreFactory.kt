package com.rick.jetpacksecurityapi37.crypto

import android.content.Context

class SecretStoreFactory(private val context: Context) {
    fun create(era: CryptoEra): SecretStore = when (era) {
        CryptoEra.LEGACY_JETSEC -> LegacySecretStore(context)
        CryptoEra.TINK_DATASTORE -> TinkSecretStore(context)
        CryptoEra.KEYSTORE_DIRECT -> KeystoreSecretStore(context)
    }
}
