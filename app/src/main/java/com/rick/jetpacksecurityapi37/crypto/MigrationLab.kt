package com.rick.jetpacksecurityapi37.crypto

import android.content.Context

class MigrationLab(private val context: Context) {

    private val legacy = LegacySecretStore(context)
    private val current = TinkSecretStore(context)

    suspend fun seedLegacySample(): String {
        legacy.save(SAMPLE_KEY, SAMPLE_VALUE)
        return "Wrote \"$SAMPLE_VALUE\" into EncryptedSharedPreferences (${LegacySecretStore.PREFS_NAME})."
    }

    suspend fun migrateLegacyToTink(): String {
        val value = legacy.read(SAMPLE_KEY)
            ?: return "Nothing to migrate. Seed the legacy store first."
        current.save(SAMPLE_KEY, value)
        context.deleteSharedPreferences(LegacySecretStore.PREFS_NAME)
        return "Copied $SAMPLE_KEY into Tink+DataStore and deleted ${LegacySecretStore.PREFS_NAME}."
    }

    suspend fun status(): String {
        val legacyValue = runCatching { legacy.read(SAMPLE_KEY) }.getOrNull()
        val currentValue = runCatching { current.read(SAMPLE_KEY) }.getOrNull()
        return buildString {
            appendLine("Legacy EncryptedSharedPreferences: ${legacyValue ?: "(empty or missing)"}")
            appendLine("Current Tink DataStore: ${currentValue ?: "(empty)"}")
        }
    }

    companion object {
        const val SAMPLE_KEY = "session_token"
        const val SAMPLE_VALUE = "legacy-token-123"
    }
}
