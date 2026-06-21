package com.helix.flashcards.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helix.flashcards.FlashCardsApp
import com.helix.flashcards.data.Card
import com.helix.flashcards.data.Deck
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onDeckClick: (Deck) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val db = (context.applicationContext as FlashCardsApp).database
    val decks by db.deckDao().getAllDecks().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var editingDeck by remember { mutableStateOf<Deck?>(null) }
    var deckName by remember { mutableStateOf("") }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }

    // Import a full deck from a JSON file exported by this app
    val importDeckPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@runCatching
                val obj = org.json.JSONObject(json)
                val name = obj.getString("name")
                val cardsArr = obj.getJSONArray("cards")
                val deckId = db.deckDao().insert(Deck(name = name))
                repeat(cardsArr.length()) { i ->
                    val c = cardsArr.getJSONObject(i)
                    db.cardDao().insert(Card(deckId = deckId, frontText = c.getString("front"), backText = c.optString("back", "")))
                }
                Toast.makeText(context, "Imported \"$name\" (${cardsArr.length()} cards)", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Import failed — not a valid deck file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CompactHeader(
                title = "FlashCards",
                actions = {
                    IconButton(onClick = { importDeckPicker.launch("application/json") }) {
                        Icon(Icons.Default.FileOpen, "Import deck", tint = AccentLight)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = AccentLight)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingDeck = null; deckName = ""; showDialog = true },
                containerColor = DarkCard,
                contentColor = AccentLight
            ) { Icon(Icons.Default.Add, "New deck") }
        },
        containerColor = DarkBg
    ) { padding ->
        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No decks yet.\nTap + to create one.", color = Accent, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(decks, key = { it.id }) { deck ->
                    val count by db.cardDao().getCardCount(deck.id).collectAsStateWithLifecycle(0)
                    DeckRow(
                        deck = deck,
                        count = count,
                        onClick = { onDeckClick(deck) },
                        onEdit = { editingDeck = deck; deckName = deck.name; showDialog = true },
                        onDelete = { deckToDelete = deck }
                    )
                }
            }
        }

        if (deckToDelete != null) {
            AlertDialog(
                onDismissRequest = { deckToDelete = null },
                containerColor = DarkSurface,
                title = { Text("Delete \"${deckToDelete!!.name}\"?") },
                text = { Text("This will delete the deck and all its cards. This can't be undone.", color = Accent) },
                confirmButton = {
                    TextButton(onClick = {
                        val deck = deckToDelete!!
                        deckToDelete = null
                        scope.launch { db.deckDao().delete(deck) }
                    }) { Text("Delete", color = Wrong) }
                },
                dismissButton = { TextButton(onClick = { deckToDelete = null }) { Text("Cancel") } }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = DarkSurface,
                title = { Text(if (editingDeck == null) "New Deck" else "Rename Deck") },
                text = {
                    OutlinedTextField(
                        value = deckName,
                        onValueChange = { deckName = it },
                        label = { Text("Deck name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentLight, cursorColor = AccentLight
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (deckName.isNotBlank()) {
                            scope.launch {
                                if (editingDeck != null) db.deckDao().update(editingDeck!!.copy(name = deckName.trim()))
                                else db.deckDao().insert(Deck(name = deckName.trim()))
                            }
                            showDialog = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun DeckRow(
    deck: Deck,
    count: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val accent = (context.applicationContext as FlashCardsApp).settings.accent

    Card(
        modifier = Modifier.responsiveWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Row(
                Modifier.weight(1f).padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(deck.name, style = MaterialTheme.typography.titleMedium, color = AccentLight)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "$count ${if (count == 1) "card" else "cards"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Accent
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = Accent) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Accent) }
            }
        }
    }
}