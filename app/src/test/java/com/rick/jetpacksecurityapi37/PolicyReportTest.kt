package com.rick.jetpacksecurityapi37

import com.rick.jetpacksecurityapi37.policy.PolicyCheck
import com.rick.jetpacksecurityapi37.policy.PolicyReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyReportTest {

    @Test
    fun blockingFailureTurnsBannerRed() {
        val report = PolicyReport.from(
            checks = listOf(
                PolicyCheck("Lock screen", passed = true, blocking = true, detail = "ok"),
                PolicyCheck("Emulator", passed = false, blocking = true, detail = "sdk"),
                PolicyCheck("ADB", passed = false, blocking = false, detail = "on"),
            ),
            allowedSummary = "ok",
            blockedPrefix = "Blocking failures:",
            allowedTitle = "ALLOWED",
            blockedTitle = "NOT ALLOWED",
        )
        assertFalse(report.allowed)
        assertEquals("NOT ALLOWED", report.blockedTitle)
        assertEquals(1, report.blockingFailures.size)
        assertEquals("Emulator", report.blockingFailures.single().name)
        assertEquals(1, report.otherFailures.size)
        assertTrue(report.summary.contains("Emulator"))
    }

    @Test
    fun optionalFailuresKeepBannerGreen() {
        val report = PolicyReport.from(
            checks = listOf(
                PolicyCheck("Lock screen", passed = true, blocking = true, detail = "ok"),
                PolicyCheck("ADB", passed = false, blocking = false, detail = "on"),
            ),
            allowedSummary = "No blocking failures.",
            blockedPrefix = "Blocking failures:",
            allowedTitle = "ALLOWED",
            blockedTitle = "NOT ALLOWED",
        )
        assertTrue(report.allowed)
        assertEquals("No blocking failures.", report.summary)
        assertTrue(report.blockingFailures.isEmpty())
        assertEquals(1, report.otherFailures.size)
    }
}
