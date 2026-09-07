package com.rick.jetpacksecurityapi37.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rick.jetpacksecurityapi37.crypto.CryptoEra

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
