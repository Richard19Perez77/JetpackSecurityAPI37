package com.rick.jetpacksecurityapi37.crypto

interface SecretStore {
    val era: CryptoEra
    suspend fun save(key: String, value: String)
    suspend fun read(key: String): String?
}

interface EncryptedBlobStore {
    val era: CryptoEra
    suspend fun write(fileName: String, plaintext: String)
    suspend fun read(fileName: String): String?
}
