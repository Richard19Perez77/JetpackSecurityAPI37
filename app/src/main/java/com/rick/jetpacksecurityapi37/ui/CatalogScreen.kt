package com.rick.jetpacksecurityapi37.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CatalogScreen(onOpen: (LabDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text("Jetpack Security · API 37", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Simple labs for review. Start with BEGINNER.md, then OVERVIEW.md. Storage and files can switch previous vs current APIs.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(LabDestination.entries) { lab ->
            TextButton(
                onClick = { onOpen(lab) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(lab.title, style = MaterialTheme.typography.titleMedium)
                    Text(lab.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
        }
    }
}
