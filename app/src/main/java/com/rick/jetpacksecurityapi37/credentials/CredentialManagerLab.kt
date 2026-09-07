package com.rick.jetpacksecurityapi37.credentials

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.exceptions.NoCredentialException

class CredentialManagerLab(private val context: Context) {

    fun inspect(): String = buildString {
        appendLine("Credential Manager is the platform neighbor for passwords and passkeys.")
        appendLine("It is not part of androidx.security, but it is the current way to store user credentials.")
        appendLine()
        val manager = runCatching { CredentialManager.create(context) }
        appendLine(
            if (manager.isSuccess) {
                "CredentialManager.create() succeeded: ${manager.getOrNull()?.javaClass?.name}"
            } else {
                "CredentialManager.create() failed: ${manager.exceptionOrNull()?.message}"
            },
        )
        appendLine()
        appendLine("GetPasswordOption is included in the sample request below. On a review emulator this often returns NoCredentialException, which is expected if no password was saved.")
    }

    suspend fun probePasswords(): String {
        val manager = CredentialManager.create(context)
        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetPasswordOption())
                .build()
            val response = manager.getCredential(context, request)
            "Received credential type=${response.credential.type}"
        } catch (e: NoCredentialException) {
            "NoCredentialException: no saved password/passkey for this app (typical on a fresh emulator)."
        } catch (t: Throwable) {
            "${t.javaClass.simpleName}: ${t.message}"
        }
    }
}
