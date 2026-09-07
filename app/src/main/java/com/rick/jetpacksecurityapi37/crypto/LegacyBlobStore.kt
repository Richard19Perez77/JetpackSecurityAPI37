package com.rick.jetpacksecurityapi37.crypto

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

@Suppress("DEPRECATION")
class LegacyBlobStore(context: Context) : EncryptedBlobStore {

    override val era = CryptoEra.LEGACY_JETSEC

    private val appContext = context.applicationContext

    private val masterKey by lazy {
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    override suspend fun write(fileName: String, plaintext: String) = withContext(Dispatchers.IO) {
        val file = EncryptedPaths.fileInDir(appContext.filesDir, DIR, fileName)
        if (file.exists()) file.delete()
        encrypted(file).openFileOutput().use { output ->
            output.write(plaintext.toByteArray(StandardCharsets.UTF_8))
        }
    }

    override suspend fun read(fileName: String): String? = withContext(Dispatchers.IO) {
        val file = EncryptedPaths.fileInDir(appContext.filesDir, DIR, fileName)
        if (!file.exists()) return@withContext null
        encrypted(file).openFileInput().use { input ->
            String(input.readBytes(), StandardCharsets.UTF_8)
        }
    }

    private fun encrypted(file: File): EncryptedFile = EncryptedFile.Builder(
        appContext,
        file,
        masterKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
    ).build()

    companion object {
        const val DIR = "jetsec_legacy_files"
    }
}
