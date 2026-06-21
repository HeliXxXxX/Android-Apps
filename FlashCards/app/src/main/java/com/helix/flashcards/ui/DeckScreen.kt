package com.helix.flashcards.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helix.flashcards.FlashCardsApp
import com.helix.flashcards.data.Card
import kotlinx.coroutines.launch

@Composable
fun DeckScreen(
    deckId: Long,
    onBack: () -> Unit,
    onStudy: () -> Unit,
    onAddCard: () -> Unit,
    onCardClick: (Card) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FlashCardsApp
    val db = app.database
    val accent = app.settings.accent
    val cards by db.cardDao().getCardsByDeck(deckId).collectAsStateWithLifecycle(emptyList())
    val deckName by produceState("") { value = db.deckDao().getDeckById(deckId)?.name ?: "" }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // Import: pick a text file, parse "front | back" per line
    val importTxtPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            var count = 0
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split("|", limit = 2).map { it.trim() }
                        val front = parts.getOrElse(0) { "" }
                        val back = parts.getOrElse(1) { "" }
                        if (front.isNotBlank()) {
                            db.cardDao().insert(Card(deckId = deckId, frontText = front, backText = back))
                            count++
                        }
                    }
                }
            }
            Toast.makeText(context, "Imported $count cards", Toast.LENGTH_SHORT).show()
        }
    }

    // Export: write "front | back" lines to a user-chosen .txt file
    val exportTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                    cards.forEach { c -> w.write("${c.frontText} | ${c.backText}"); w.newLine() }
                }
            }
            Toast.makeText(context, "Exported ${cards.size} cards", Toast.LENGTH_SHORT).show()
        }
    }

    // Export: write full deck as JSON (name + cards) to a user-chosen .json file
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val arr = org.json.JSONArray()
                cards.forEach { c ->
                    arr.put(org.json.JSONObject().put("front", c.frontText).put("back", c.backText))
                }
                val obj = org.json.JSONObject().put("name", deckName).put("cards", arr)
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                    w.write(obj.toString(2))
                }
            }
            Toast.makeText(context, "Deck exported", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CompactHeader(
                title = deckName,
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Options", tint = Accent)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import cards (.txt)", color = AccentLight) },
                                leadingIcon = { Icon(Icons.Default.FileOpen, null, tint = Accent) },
                                onClick = { showMenu = false; importTxtPicker.launch("text/*") }
                            )
                            DropdownMenuItem(
                                text = { Text("Export cards (.txt)", color = AccentLight) },
                                leadingIcon = { Icon(Icons.Default.Download, null, tint = Accent) },
                                onClick = { showMenu = false; exportTxtLauncher.launch("$deckName.txt") }
                            )
                            DropdownMenuItem(
                                text = { Text("Export deck (.json)", color = AccentLight) },
                                leadingIcon = { Icon(Icons.Default.Share, null, tint = Accent) },
                                onClick = { showMenu = false; exportJsonLauncher.launch("$deckName.json") }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(
                    onClick = onAddCard,
                    containerColor = DarkSurface,
                    contentColor = AccentLight
                ) { Icon(Icons.Default.Add, "Add card") }

                if (cards.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = onStudy,
                        containerColor = DarkCard,
                        contentColor = AccentLight
                    ) {
                        Text("Study")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.PlayArrow, null)
                    }
                }
            }
        },
        containerColor = accent.copy(alpha = 0.12f).compositeOver(DarkBg)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (cards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No cards yet.\nTap + to add one.", color = Accent, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    Modifier.responsiveWidth().fillMaxHeight(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(cards, key = { _, c -> c.id }) { index, card ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onCardClick(card) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Row(
                                Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    cardLabel(index, card.frontText),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AccentLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Accent)
                            }
                        }
                    }
                }
            }
        }
    }
}