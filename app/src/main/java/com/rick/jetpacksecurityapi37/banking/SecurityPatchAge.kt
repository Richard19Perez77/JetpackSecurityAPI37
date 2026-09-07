package com.rick.jetpacksecurityapi37.banking

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Parses `Build.VERSION.SECURITY_PATCH` (`yyyy-MM-dd`) without depending on Android types,
 * so the cutoff logic can be unit-tested on the host JVM.
 */
internal object SecurityPatchAge {

    fun ageDays(patch: String, nowMillis: Long): Long? {
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(patch)
        }.getOrNull() ?: return null
        return TimeUnit.MILLISECONDS.toDays(nowMillis - parsed.time)
    }

    fun isFresh(patch: String, nowMillis: Long, maxAgeDays: Long): Boolean {
        val age = ageDays(patch, nowMillis) ?: return false
        return age <= maxAgeDays
    }
}
