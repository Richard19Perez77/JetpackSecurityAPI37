package com.rick.jetpacksecurityapi37.backup

import android.content.Context
import android.os.Build
import com.rick.jetpacksecurityapi37.crypto.KeystoreBlobStore
import com.rick.jetpacksecurityapi37.crypto.KeystoreSecretStore
import com.rick.jetpacksecurityapi37.crypto.LegacyBlobStore
import com.rick.jetpacksecurityapi37.crypto.LegacySecretStore
import com.rick.jetpacksecurityapi37.crypto.TinkBlobStore
import com.rick.jetpacksecurityapi37.crypto.TinkSecretStore

class BackupLab(private val context: Context) {

    fun inspect(): String = buildString {
        val info = context.applicationInfo
        appendLine("applicationInfo.flags BACKUP_ALLOWED: ${(info.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0}")
        appendLine("SDK: ${Build.VERSION.SDK_INT} (fullBackupContent used below 31, dataExtractionRules on 31+)")
        appendLine()
        appendLine("Encrypted blobs must not restore onto a new device whose Keystore is empty.")
        appendLine("This app keeps allowBackup=true and excludes the files listed in backup_rules.xml / data_extraction_rules.xml:")
        appendLine()
        listOf(
            "sharedpref ${LegacySecretStore.PREFS_NAME}",
            "sharedpref ${TinkSecretStore.KEYSET_PREFS}",
            "sharedpref ${TinkBlobStore.KEYSET_PREFS}",
            "sharedpref ${KeystoreSecretStore.PREFS_NAME}",
            "file ${LegacyBlobStore.DIR}/",
            "file ${TinkBlobStore.DIR}/",
            "file ${KeystoreBlobStore.DIR}/",
            "file datastore/${TinkSecretStore.DATASTORE_NAME}.preferences_pb",
        ).forEach { appendLine("- $it") }
    }
}
