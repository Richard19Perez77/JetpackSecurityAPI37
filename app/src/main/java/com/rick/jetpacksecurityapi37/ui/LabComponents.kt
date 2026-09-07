package com.rick.jetpacksecurityapi37.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rick.jetpacksecurityapi37.crypto.CryptoEra
import com.rick.jetpacksecurityapi37.policy.PolicyReport

@Composable
fun LabColumn(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            content()
        },
    )
}

@Composable
fun EraPicker(
    selected: CryptoEra,
    onSelect: (CryptoEra) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("API era", style = MaterialTheme.typography.titleSmall)
        CryptoEra.entries.forEach { era ->
            val prefix = if (era == selected) "● " else "○ "
            TextButton(onClick = { onSelect(era) }, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(prefix + era.title)
                    Text(era.summary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun LabButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Composable
fun ResultText(text: String) {
    if (text.isNotBlank()) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PolicyLabScreen(
    title: String,
    body: String,
    onEvaluate: () -> PolicyReport,
) {
    var report by remember { mutableStateOf<PolicyReport?>(null) }
    LabColumn(title = title, body = body) {
        LabButton("Evaluate this device") { report = onEvaluate() }
        report?.let { PolicyReportView(it) }
    }
}

@Composable
fun PolicyReportView(report: PolicyReport) {
    val background = if (report.allowed) Color(0xFF2E7D32) else Color(0xFFC62828)
    Text(
        text = if (report.allowed) report.allowedTitle else report.blockedTitle,
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(16.dp),
    )
    Text(report.summary, style = MaterialTheme.typography.bodyMedium)
    report.checks.forEach { check ->
        val mark = if (check.passed) "PASS" else "FAIL"
        val weight = if (check.blocking) "blocking" else "optional"
        Text(
            "$mark · $weight · ${check.name}\n${check.detail}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
