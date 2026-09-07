package com.rick.jetpacksecurityapi37.banking

data class DeviceCheck(
    val name: String,
    val passed: Boolean,
    val blocking: Boolean,
    val detail: String,
)

data class BankingReadinessReport(
    val allowed: Boolean,
    val summary: String,
    val checks: List<DeviceCheck>,
) {
    val blockingFailures: List<DeviceCheck> get() = checks.filter { it.blocking && !it.passed }
    val otherFailures: List<DeviceCheck> get() = checks.filter { !it.blocking && !it.passed }
}
