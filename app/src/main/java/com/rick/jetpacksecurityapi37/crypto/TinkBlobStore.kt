package com.rick.jetpacksecurityapi37.crypto

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class TinkBlobStore(context: Context) : EncryptedBlobStore {

    override val era = CryptoEra.TINK_DATASTORE

    private val appContext = context.applicationContext
    private val streamingAead: StreamingAead by lazy { createStreamingAead() }

    override suspend fun write(fileName: String, plaintext: String) = withContext(Dispatchers.IO) {
        val file = EncryptedPaths.fileInDir(appContext.filesDir, DIR, fileName)
        if (file.exists()) file.delete()
        val associatedData = fileName.toByteArray(StandardCharsets.UTF_8)
        file.outputStream().use { fileOut ->
            streamingAead.newEncryptingStream(fileOut, associatedData).use { encrypting ->
                encrypting.write(plaintext.toByteArray(StandardCharsets.UTF_8))
            }
        }
    }

    override suspend fun read(fileName: String): String? = withContext(Dispatchers.IO) {
        val file = EncryptedPaths.fileInDir(appContext.filesDir, DIR, fileName)
        if (!file.exists()) return@withContext null
        val associatedData = fileName.toByteArray(StandardCharsets.UTF_8)
        file.inputStream().use { fileIn ->
            streamingAead.newDecryptingStream(fileIn, associatedData).use { decrypting ->
                String(decrypting.readBytes(), StandardCharsets.UTF_8)
            }
        }
    }

    private fun createStreamingAead(): StreamingAead {
        StreamingAeadConfig.register()
        return AndroidKeysetManager.Builder()
            .withSharedPref(appContext, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
    }

    companion object {
        const val DIR = "jetsec_tink_files"
        const val KEYSET_PREFS = "jetsec_tink_stream_keyset_prefs"
        const val KEYSET_NAME = "jetsec_tink_stream_keyset"
        const val MASTER_KEY_URI = "android-keystore://jetsec_tink_stream_master"
    }
}
