package com.rick.jetpacksecurityapi37.policy

/**
 * One named row in a real-world policy lab (PASS/FAIL + blocking vs optional).
 *
 * @property name Short label shown in the lab.
 * @property passed Whether this device or window met the check.
 * @property blocking If true, a failure turns the banner red.
 * @property detail Why the check passed or failed.
 */
data class PolicyCheck(
    val name: String,
    val passed: Boolean,
    val blocking: Boolean,
    val detail: String,
)

data class PolicyReport(
    val allowed: Boolean,
    val summary: String,
    val checks: List<PolicyCheck>,
    val allowedTitle: String = "ALLOWED",
    val blockedTitle: String = "NOT ALLOWED",
) {
    val blockingFailures: List<PolicyCheck> get() = checks.filter { it.blocking && !it.passed }
    val otherFailures: List<PolicyCheck> get() = checks.filter { !it.blocking && !it.passed }

    companion object {
        fun from(
            checks: List<PolicyCheck>,
            allowedSummary: String,
            blockedPrefix: String,
            allowedTitle: String,
            blockedTitle: String,
        ): PolicyReport {
            val allowed = checks.none { it.blocking && !it.passed }
            val summary = if (allowed) {
                allowedSummary
            } else {
                val names = checks.filter { it.blocking && !it.passed }.joinToString { it.name }
                "$blockedPrefix $names."
            }
            return PolicyReport(
                allowed = allowed,
                summary = summary,
                checks = checks,
                allowedTitle = allowedTitle,
                blockedTitle = blockedTitle,
            )
        }
    }
}
