package com.helix.flashcards.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.helix.flashcards.FlashCardsApp
import com.helix.flashcards.data.Card
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CardViewScreen(
    deckId: Long,
    cardId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FlashCardsApp
    val db = app.database
    val accent = app.settings.accent
    val scope = rememberCoroutineScope()

    val card by db.cardDao().getCardByIdFlow(cardId).collectAsStateWithLifecycle(null)
    val cards by db.cardDao().getCardsByDeck(deckId).collectAsStateWithLifecycle(emptyList())
    val position = cards.indexOfFirst { it.id == cardId }

    var confirmDelete by remember { mutableStateOf(false) }

    val title = if (position >= 0) "Card ${(position + 1).toString().padStart(2, '0')}" else "Card"

    Scaffold(
        topBar = {
            CompactHeader(
                title = title,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = Accent)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onEdit,
                containerColor = DarkCard,
                contentColor = AccentLight,
                icon = { Icon(Icons.Default.Edit, null) },
                text = { Text("Edit") }
            )
        },
        containerColor = accent.copy(alpha = 0.12f).compositeOver(DarkBg)
    ) { padding ->
        val c = card
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (c == null) {
                Text("Card not found", color = Accent, modifier = Modifier.padding(24.dp))
            } else {
                Column(
                    Modifier
                        .responsiveWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FaceSection("FRONT", c.frontText, c.frontImageUri)
                    FaceSection("BACK", c.backText, c.backImageUri)
                    Spacer(Modifier.height(72.dp))
                }
            }
        }
    }

    if (confirmDelete && card != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = DarkSurface,
            title = { Text("Delete card?") },
            text = { Text("This can't be undone.", color = Accent) },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = card!!
                    confirmDelete = false
                    scope.launch { db.cardDao().delete(toDelete); onBack() }
                }) { Text("Delete", color = Wrong) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FaceSection(label: String, text: String, imageUri: String?) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = Accent)
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = File(imageUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
                if (text.isNotBlank()) Spacer(Modifier.height(12.dp))
            }
            if (text.isNotBlank()) {
                Text(text, color = AccentLight, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            } else if (imageUri == null) {
                Text("—", color = Accent, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}