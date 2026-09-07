package com.rick.jetpacksecurityapi37.authenticator.testing

import android.content.Context
import androidx.annotation.XmlRes
import androidx.security.app.authenticator.AppAuthenticator
import androidx.security.app.authenticator.TestAppAuthenticatorBuilder

/**
 * Builds injectable [AppAuthenticator] instances for instrumented tests.
 *
 * Production code should use [AppAuthenticator.createFromResource] against real
 * signing certificates. Tests should not depend on the debug keystore digest.
 */
object TestAuthenticatorFactory {

    fun acceptDeclaredPackages(
        context: Context,
        @XmlRes xmlResId: Int,
    ): AppAuthenticator = TestAppAuthenticatorBuilder.createFromResource(context, xmlResId)
        .setTestPolicy(TestAppAuthenticatorBuilder.POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES)
        .build()

    fun denyAll(
        context: Context,
        @XmlRes xmlResId: Int,
    ): AppAuthenticator = TestAppAuthenticatorBuilder.createFromResource(context, xmlResId)
        .setTestPolicy(TestAppAuthenticatorBuilder.POLICY_DENY_ALL)
        .build()
}
