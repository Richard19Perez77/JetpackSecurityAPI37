package com.rick.jetpacksecurityapi37.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreBlobStore(context: Context) : EncryptedBlobStore {

    override val era = CryptoEra.KEYSTORE_DIRECT

    private val appContext = context.applicationContext

    override suspend fun write(fileName: String, plaintext: String) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val file = fileFor(fileName)
        file.parentFile?.mkdirs()
        file.writeBytes(cipher.iv + ciphertext)
    }

    override suspend fun read(fileName: String): String? = withContext(Dispatchers.IO) {
        val file = fileFor(fileName)
        if (!file.exists()) return@withContext null
        val packed = file.readBytes()
        require(packed.size > IV_SIZE) { "Ciphertext too short" }
        val iv = packed.copyOfRange(0, IV_SIZE)
        val ciphertext = packed.copyOfRange(IV_SIZE, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, aesKey(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    private fun fileFor(fileName: String): File {
        val dir = File(appContext.filesDir, DIR).apply { mkdirs() }
        return File(dir, fileName)
    }

    private fun aesKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val DIR = "jetsec_keystore_files"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "jetsec_direct_file_aes"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
