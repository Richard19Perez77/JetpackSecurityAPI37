package com.rick.jetpacksecurityapi37.ui.labs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.rick.jetpacksecurityapi37.authenticator.AppAuthenticatorLab
import com.rick.jetpacksecurityapi37.backup.BackupLab
import com.rick.jetpacksecurityapi37.biometric.BiometricKeyLab
import com.rick.jetpacksecurityapi37.credentials.CredentialManagerLab
import com.rick.jetpacksecurityapi37.crypto.MigrationLab
import com.rick.jetpacksecurityapi37.identity.IdentityCredentialLab
import com.rick.jetpacksecurityapi37.keystore.KeystoreInfoLab
import com.rick.jetpacksecurityapi37.state.SecurityStateLab
import com.rick.jetpacksecurityapi37.ui.LabButton
import com.rick.jetpacksecurityapi37.ui.LabColumn
import com.rick.jetpacksecurityapi37.ui.ResultText
import kotlinx.coroutines.launch

@Composable
fun MigrationLabScreen() {
    val context = LocalContext.current
    val lab = remember { MigrationLab(context) }
    var result by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LabColumn(
        title = "Crypto migration",
        body = "Seeds the deprecated EncryptedSharedPreferences file, copies the value into Tink+DataStore, then deletes the legacy prefs file.",
    ) {
        LabButton("1. Seed legacy prefs") {
            scope.launch { result = runCatching { lab.seedLegacySample() }.getOrElse { it.message.orEmpty() } }
        }
        LabButton("2. Migrate to Tink") {
            scope.launch { result = runCatching { lab.migrateLegacyToTink() }.getOrElse { it.message.orEmpty() } }
        }
        LabButton("Show both stores") {
            scope.launch { result = runCatching { lab.status() }.getOrElse { it.message.orEmpty() } }
        }
        ResultText(result)
    }
}

@Composable
fun KeystoreLabScreen() {
    val context = LocalContext.current
    val lab = remember { KeystoreInfoLab(context) }
    var result by remember { mutableStateOf("") }

    LabColumn(
        title = "Android Keystore",
        body = "Jetpack Security wraps this. The lab creates two AES keys: default Keystore, then StrongBox if the device has the feature.",
    ) {
        LabButton("Inspect keys") { result = lab.inspect() }
        ResultText(result)
    }
}

@Composable
fun AuthenticatorLabScreen() {
    val context = LocalContext.current
    val lab = remember { AppAuthenticatorLab(context) }
    var result by remember { mutableStateOf("") }

    LabColumn(
        title = "App Authenticator",
        body = "Checks this app’s signing cert against XML. Instrumented tests in :authenticator-testing inject MATCH / DENY_ALL policies so you do not need a second APK.",
    ) {
        LabButton("Run identity checks") { result = lab.inspect() }
        ResultText(result)
    }
}

@Composable
fun IdentityLabScreen() {
    val context = LocalContext.current
    val lab = remember { IdentityCredentialLab(context) }
    var result by remember { mutableStateOf("") }

    LabColumn(
        title = "Identity Credential",
        body = "Probes the Jetpack store (software fallback) and the platform hardware store on API 30+. Creating a full mDL needs an issuer; this lab only creates a CredentialKey.",
    ) {
        LabButton("Probe stores") { result = lab.inspect() }
        ResultText(result)
    }
}

@Composable
fun SecurityStateLabScreen() {
    val context = LocalContext.current
    val lab = remember { SecurityStateLab(context) }
    var result by remember { mutableStateOf("") }

    LabColumn(
        title = "Security State",
        body = "Reads whatever SecurityStateManagerCompat returns on this device, then shows a scripted OEM UpdateInfo sample. Provider APIs are privileged.",
    ) {
        LabButton("Read state") { result = lab.inspect() }
        ResultText(result)
    }
}

@Composable
fun BiometricLabScreen() {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val lab = remember { BiometricKeyLab(context) }
    var result by remember { mutableStateOf(lab.availability()) }
    val scope = rememberCoroutineScope()

    LabColumn(
        title = "Biometric-gated key",
        body = "Creates an AndroidKeyStore AES key that requires BIOMETRIC_STRONG. Emulators without enrolled biometrics will fail the prompt; that is expected.",
    ) {
        LabButton("Check availability") { result = lab.availability() }
        LabButton("Encrypt sample") {
            scope.launch {
                result = if (activity == null) {
                    "Host activity is not a FragmentActivity."
                } else {
                    runCatching { lab.encryptSample(activity) }.getOrElse {
                        "${it.javaClass.simpleName}: ${it.message}"
                    }
                }
            }
        }
        ResultText(result)
    }
}

@Composable
fun CredentialManagerLabScreen() {
    val context = LocalContext.current
    val lab = remember { CredentialManagerLab(context) }
    var result by remember { mutableStateOf(lab.inspect()) }
    val scope = rememberCoroutineScope()

    LabColumn(
        title = "Credential Manager",
        body = "Asks the system for a saved password. A fresh device typically throws NoCredentialException. Passkeys need a Digital Asset Links setup that this review app does not use.",
    ) {
        LabButton("Describe API") { result = lab.inspect() }
        LabButton("Get password credential") {
            scope.launch {
                result = runCatching { lab.probePasswords() }.getOrElse {
                    "${it.javaClass.simpleName}: ${it.message}"
                }
            }
        }
        ResultText(result)
    }
}

@Composable
fun BackupLabScreen() {
    val context = LocalContext.current
    val lab = remember { BackupLab(context) }
    var result by remember { mutableStateOf("") }

    LabColumn(
        title = "Backup exclusion",
        body = "Auto Backup restoring ciphertext without the original Keystore keys crashes readers. The XML rules exclude this app’s encrypted files.",
    ) {
        LabButton("Show backup policy") { result = lab.inspect() }
        ResultText(result)
    }
}
