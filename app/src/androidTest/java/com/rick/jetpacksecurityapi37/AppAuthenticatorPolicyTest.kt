package com.rick.jetpacksecurityapi37

import androidx.security.app.authenticator.AppAuthenticator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rick.jetpacksecurityapi37.authenticator.testing.TestAuthenticatorFactory
import com.rick.jetpacksecurityapi37.test.R as TestR
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppAuthenticatorPolicyTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun declaredPackageAcceptedUnderAllowPolicy() {
        val authenticator = TestAuthenticatorFactory.acceptDeclaredPackages(
            context,
            TestR.xml.test_app_authenticator,
        )
        assertEquals(
            AppAuthenticator.SIGNATURE_MATCH,
            authenticator.checkAppIdentity(context.packageName),
        )
    }

    @Test
    fun declaredPackageDeniedUnderDenyAllPolicy() {
        val authenticator = TestAuthenticatorFactory.denyAll(
            context,
            TestR.xml.test_app_authenticator,
        )
        assertEquals(
            AppAuthenticator.SIGNATURE_NO_MATCH,
            authenticator.checkAppIdentity(context.packageName),
        )
    }
}
