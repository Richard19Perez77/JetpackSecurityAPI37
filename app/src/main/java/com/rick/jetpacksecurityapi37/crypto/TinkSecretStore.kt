package com.rick.jetpacksecurityapi37.crypto

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first
import java.nio.charset.StandardCharsets

private val Context.tinkSecretDataStore: DataStore<Preferences> by preferencesDataStore(
    name = TinkSecretStore.DATASTORE_NAME,
)

class TinkSecretStore(context: Context) : SecretStore {

    override val era = CryptoEra.TINK_DATASTORE

    private val appContext = context.applicationContext
    private val aead: Aead

    init {
        AeadConfig.register()
        aead = AndroidKeysetManager.Builder()
            .withSharedPref(appContext, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override suspend fun save(key: String, value: String) {
        val ciphertext = aead.encrypt(
            value.toByteArray(StandardCharsets.UTF_8),
            key.toByteArray(StandardCharsets.UTF_8),
        )
        val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        appContext.tinkSecretDataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = encoded
        }
    }

    override suspend fun read(key: String): String? {
        val encoded = appContext.tinkSecretDataStore.data.first()[stringPreferencesKey(key)]
            ?: return null
        val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
        val plaintext = aead.decrypt(
            ciphertext,
            key.toByteArray(StandardCharsets.UTF_8),
        )
        return String(plaintext, StandardCharsets.UTF_8)
    }

    companion object {
        const val DATASTORE_NAME = "jetsec_tink_secrets"
        const val KEYSET_PREFS = "jetsec_tink_keyset_prefs"
        const val KEYSET_NAME = "jetsec_tink_aead_keyset"
        const val MASTER_KEY_URI = "android-keystore://jetsec_tink_master"
    }
}
