package com.rick.jetpacksecurityapi37.crypto

import android.content.Context

class BlobStoreFactory(private val context: Context) {
    fun create(era: CryptoEra): EncryptedBlobStore = when (era) {
        CryptoEra.LEGACY_JETSEC -> LegacyBlobStore(context)
        CryptoEra.TINK_DATASTORE -> TinkBlobStore(context)
        CryptoEra.KEYSTORE_DIRECT -> KeystoreBlobStore(context)
    }
}
