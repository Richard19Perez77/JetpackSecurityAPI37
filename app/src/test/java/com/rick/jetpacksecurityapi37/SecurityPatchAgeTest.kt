package com.rick.jetpacksecurityapi37

import com.rick.jetpacksecurityapi37.banking.SecurityPatchAge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SecurityPatchAgeTest {

    @Test
    fun parsesIsoDate() {
        val now = 1_778_000_000_000L
        val patch = "2026-01-01"
        val age = SecurityPatchAge.ageDays(patch, now)
        assertTrue(age != null && age >= 0)
    }

    @Test
    fun freshWithinCutoff() {
        val now = 1_778_000_000_000L
        val recent = now - TimeUnit.DAYS.toMillis(10)
        val patch = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(recent))
        assertTrue(SecurityPatchAge.isFresh(patch, now, 365))
    }

    @Test
    fun staleAfterCutoff() {
        val now = 1_778_000_000_000L
        val old = now - TimeUnit.DAYS.toMillis(400)
        val patch = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(old))
        assertFalse(SecurityPatchAge.isFresh(patch, now, 365))
    }

    @Test
    fun rejectsGarbage() {
        assertNull(SecurityPatchAge.ageDays("not-a-date", 0L))
        assertFalse(SecurityPatchAge.isFresh("2026-13-99", 0L, 365))
    }

    @Test
    fun ageDaysMatchesClock() {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            isLenient = false
        }
        val start = format.parse("2026-01-01")!!.time
        val now = start + TimeUnit.DAYS.toMillis(40)
        assertEquals(40L, SecurityPatchAge.ageDays("2026-01-01", now))
    }
}
