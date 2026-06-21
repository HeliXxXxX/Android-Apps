package com.helix.flashcards.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Scaffold(
        topBar = { CompactHeader(title = "About", onBack = onBack) },
        containerColor = DarkBg
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .responsiveWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("FlashCards", style = MaterialTheme.typography.headlineMedium, color = AccentLight)
                Text("Version $version", style = MaterialTheme.typography.bodyMedium, color = Accent)

                InfoCard(title = "Notes from the creator") {
                    Text(
                        "Thanks for using FlashCards. This space is for notes, updates, and " +
                                "what's new — coming soon.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AccentLight
                    )
                }

                InfoCard(title = "Details") {
                    InfoRow("App", "FlashCards")
                    InfoRow("Version", version)
                    InfoRow("Made with", "Kotlin · Jetpack Compose")
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Accent)
    Surface(color = DarkCard, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Accent)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AccentLight)
    }
}