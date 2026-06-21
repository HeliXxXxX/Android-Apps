package com.helix.flashcards.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helix.flashcards.FlashCardsApp

@Composable
fun DeckPickerScreen(onPick: (Long) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val db = (context.applicationContext as FlashCardsApp).database
    val decks by db.deckDao().getAllDecks().collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = { CompactHeader(title = "Add to deck", onBack = onCancel) },
        containerColor = DarkBg
    ) { padding ->
        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No decks yet.\nCreate one first.", color = Accent, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(decks, key = { it.id }) { deck ->
                    Card(
                        modifier = Modifier.responsiveWidth().clickable { onPick(deck.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Text(
                            deck.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentLight,
                            modifier = Modifier.padding(18.dp).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}