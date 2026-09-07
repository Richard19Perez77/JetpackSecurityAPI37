package com.rick.jetpacksecurityapi37.ui.labs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rick.jetpacksecurityapi37.crypto.BlobStoreFactory
import com.rick.jetpacksecurityapi37.crypto.CryptoEra
import com.rick.jetpacksecurityapi37.crypto.SecretStoreFactory
import com.rick.jetpacksecurityapi37.ui.EraPicker
import com.rick.jetpacksecurityapi37.ui.LabButton
import com.rick.jetpacksecurityapi37.ui.LabColumn
import com.rick.jetpacksecurityapi37.ui.ResultText
import kotlinx.coroutines.launch

private const val SAMPLE_KEY = "api_token"
private const val SAMPLE_VALUE = "review-secret"
private const val SAMPLE_FILE = "note.txt"

@Composable
fun EncryptedPreferencesLab() {
    val context = LocalContext.current
    val factory = remember { SecretStoreFactory(context) }
    var era by remember { mutableStateOf(CryptoEra.TINK_DATASTORE) }
    var result by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LabColumn(
        title = "Encrypted preferences",
        body = "Same save/read buttons. Change the era to run the previous Jetpack Security crypto APIs, the current Tink+DataStore path, or raw Keystore AES-GCM.",
    ) {
        EraPicker(era) { era = it }
        LabButton("Save sample") {
            scope.launch {
                result = runCatching {
                    factory.create(era).save(SAMPLE_KEY, SAMPLE_VALUE)
                    "Saved \"$SAMPLE_VALUE\" with ${era.title}"
                }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }
            }
        }
        LabButton("Read sample") {
            scope.launch {
                result = runCatching {
                    val value = factory.create(era).read(SAMPLE_KEY)
                    "Read: ${value ?: "(empty)"}"
                }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }
            }
        }
        ResultText(result)
    }
}

@Composable
fun EncryptedFilesLab() {
    val context = LocalContext.current
    val factory = remember { BlobStoreFactory(context) }
    var era by remember { mutableStateOf(CryptoEra.TINK_DATASTORE) }
    var result by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LabColumn(
        title = "Encrypted files",
        body = "Writes a small ciphertext file under app filesDir. EncryptedFile is the previous helper. StreamingAead is the current Tink primitive. Keystore AES-GCM is the platform neighbor.",
    ) {
        EraPicker(era) { era = it }
        LabButton("Write sample file") {
            scope.launch {
                result = runCatching {
                    factory.create(era).write(SAMPLE_FILE, SAMPLE_VALUE)
                    "Wrote $SAMPLE_FILE with ${era.title}"
                }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }
            }
        }
        LabButton("Read sample file") {
            scope.launch {
                result = runCatching {
                    val value = factory.create(era).read(SAMPLE_FILE)
                    "Read: ${value ?: "(missing)"}"
                }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }
            }
        }
        ResultText(result)
    }
}
