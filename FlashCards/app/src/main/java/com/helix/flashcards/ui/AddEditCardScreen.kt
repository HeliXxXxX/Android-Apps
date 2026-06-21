package com.helix.flashcards.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.helix.flashcards.FlashCardsApp
import com.helix.flashcards.data.Card
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private val sentenceCaps = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
fun AddEditCardScreen(
    deckId: Long,
    cardId: Long?,
    sharedText: String? = null,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FlashCardsApp
    val db = app.database
    val accent = app.settings.accent
    val scope = rememberCoroutineScope()

    var frontText by rememberSaveable { mutableStateOf(sharedText ?: "") }
    var backText by rememberSaveable { mutableStateOf("") }
    var frontImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var backImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var drawingSide by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(cardId) {
        if (!loaded) {
            if (cardId != null) {
                db.cardDao().getCardById(cardId)?.let { card ->
                    frontText = card.frontText
                    backText = card.backText
                    frontImageUri = card.frontImageUri
                    backImageUri = card.backImageUri
                }
            }
            loaded = true
        }
    }

    fun copyImage(uri: Uri): String {
        val outFile = File(context.filesDir, "img_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        return outFile.absolutePath
    }

    val frontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { frontImageUri = copyImage(it) }
    }
    val backPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { backImageUri = copyImage(it) }
    }

    if (!loaded) return

    if (drawingSide != null) {
        HandwritingScreen(
            onSave = { path ->
                if (drawingSide == "front") frontImageUri = path else backImageUri = path
                drawingSide = null
            },
            onCancel = { drawingSide = null }
        )
        return
    }

    fun save() {
        if (frontText.isNotBlank() || frontImageUri != null) {
            scope.launch {
                val card = Card(
                    id = cardId ?: 0,
                    deckId = deckId,
                    frontText = frontText.trim(),
                    frontImageUri = frontImageUri,
                    backText = backText.trim(),
                    backImageUri = backImageUri
                )
                if (cardId != null) db.cardDao().update(card) else db.cardDao().insert(card)
                onDone()
            }
        }
    }

    Scaffold(
        topBar = {
            CompactHeader(title = if (cardId == null) "New Card" else "Edit Card", onBack = onDone)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { save() },
                containerColor = DarkCard,
                contentColor = AccentLight,
                modifier = Modifier.imePadding()
            ) { Icon(Icons.Default.Check, "Save") }
        },
        containerColor = accent.copy(alpha = 0.12f).compositeOver(DarkBg)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .responsiveWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("FRONT", style = MaterialTheme.typography.labelMedium, color = Accent)
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = frontText,
                            onValueChange = { frontText = it },
                            label = { Text("Text (optional)") },
                            keyboardOptions = sentenceCaps,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLight, cursorColor = AccentLight)
                        )
                        ImageSlot(frontImageUri, { frontPicker.launch("image/*") }, { drawingSide = "front" }, { frontImageUri = null })
                    }
                }

                Text("BACK", style = MaterialTheme.typography.labelMedium, color = Accent)
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = backText,
                            onValueChange = { backText = it },
                            label = { Text("Text (optional)") },
                            keyboardOptions = sentenceCaps,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLight, cursorColor = AccentLight)
                        )
                        ImageSlot(backImageUri, { backPicker.launch("image/*") }, { drawingSide = "back" }, { backImageUri = null })
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageSlot(imageUri: String?, onPickImage: () -> Unit, onDraw: () -> Unit, onClear: () -> Unit) {
    if (imageUri != null) {
        Box(Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(8.dp))) {
            AsyncImage(
                model = File(imageUri),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            IconButton(onClick = onClear, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Close, "Remove", tint = AccentLight)
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SlotButton("Image", Icons.Default.Image, Modifier.weight(1f), onPickImage)
            SlotButton("Draw", Icons.Default.Draw, Modifier.weight(1f), onDraw)
        }
    }
}

@Composable
private fun SlotButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = modifier.height(72.dp).clickable { onClick() },
        colors = CardDefaults.outlinedCardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCard))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, label, tint = Accent)
                Text(label, color = Accent)
            }
        }
    }
}