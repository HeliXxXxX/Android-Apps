package com.helix.flashcards.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.helix.flashcards.FlashCardsApp
import com.helix.flashcards.data.Card
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class Phase { STUDY, END }

@Composable
fun StudyScreen(deckId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FlashCardsApp
    val db = app.database
    val accent = app.settings.accent
    val allCards by db.cardDao().getCardsByDeck(deckId).collectAsStateWithLifecycle(emptyList())

    val prefs = remember { context.getSharedPreferences("fc_removed", android.content.Context.MODE_PRIVATE) }
    val prefsKey = "deck_$deckId"

    fun loadRemoved(): Set<Long> =
        prefs.getStringSet(prefsKey, emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    fun saveRemoved(ids: Set<Long>) =
        prefs.edit().putStringSet(prefsKey, ids.map { it.toString() }.toSet()).apply()

    var removedIds by remember { mutableStateOf(loadRemoved()) }
    var roundCards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.STUDY) }
    var isFlipped by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    fun startRound(pool: List<Card>) {
        roundCards = pool.shuffled()
        index = 0
        correct = 0
        wrong = 0
        isFlipped = false
        phase = if (roundCards.isEmpty()) Phase.END else Phase.STUDY
    }

    LaunchedEffect(allCards) {
        if (!initialized && allCards.isNotEmpty()) {
            startRound(allCards.filter { it.id !in removedIds })
            initialized = true
        }
    }

    fun advance() {
        isFlipped = false
        if (index < roundCards.lastIndex) index++ else phase = Phase.END
    }

    fun onRight() { correct++; advance() }
    fun onLeft() { wrong++; advance() }
    fun onDown() {
        val card = roundCards.getOrNull(index) ?: return
        removedIds = removedIds + card.id
        saveRemoved(removedIds)
        advance()
    }

    fun repeat() = startRound(allCards.filter { it.id !in removedIds })
    fun resetAll() {
        removedIds = emptySet()
        saveRemoved(emptySet())
        startRound(allCards)
    }

    Scaffold(
        topBar = {
            CompactHeader(
                title = "Study",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { resetAll() }) { Text("Reset", color = AccentLight) }
                }
            )
        },
        containerColor = accent.copy(alpha = 0.12f).compositeOver(DarkBg)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            BoxWithConstraints(
                Modifier.responsiveWidth().fillMaxHeight().padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                val cardW = minOf(maxWidth * 0.97f, 480.dp)
                val cardH = minOf(maxHeight * 0.85f, 680.dp)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CounterBox(
                        correct = correct,
                        wrong = wrong,
                        removed = removedIds.size,
                        left = (roundCards.size - index).coerceAtLeast(0),
                        width = cardW
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val card = roundCards.getOrNull(index)
                        val nextCard = roundCards.getOrNull(index + 1)
                        when {
                            allCards.isEmpty() -> CenterText("No cards in this deck")

                            phase == Phase.END -> EndCard(
                                correct = correct,
                                wrong = wrong,
                                removed = removedIds.size,
                                width = cardW,
                                height = cardH,
                                onRepeat = { repeat() },
                                onClose = onBack,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            card == null -> {}

                            else -> CardStack(
                                card = card,
                                nextCard = nextCard,
                                isFlipped = isFlipped,
                                width = cardW,
                                height = cardH,
                                onTap = { isFlipped = !isFlipped },
                                onRight = { onRight() },
                                onLeft = { onLeft() },
                                onDown = { onDown() },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterBox(correct: Int, wrong: Int, removed: Int, left: Int, width: Dp) {
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.width(width)
    ) {
        Row(
            Modifier.padding(vertical = 14.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Chip("Correct", correct, CorrectText)
            Chip("Wrong", wrong, WrongText)
            Chip("Removed", removed, CounterSoft)
            Chip("Left", left, AccentLight)
        }
    }
}

@Composable
private fun Chip(label: String, count: Int, numberColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", color = numberColor, style = MaterialTheme.typography.labelLarge)
        Text(label, color = Accent, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = AccentLight, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CardStack(
    card: Card,
    nextCard: Card?,
    isFlipped: Boolean,
    width: Dp,
    height: Dp,
    onTap: () -> Unit,
    onRight: () -> Unit,
    onLeft: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var flipSign by remember { mutableFloatStateOf(1f) }

    val flipProgress by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(420),
        label = "flip"
    )

    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var rawX by remember { mutableFloatStateOf(0f) }
    var rawY by remember { mutableFloatStateOf(0f) }
    val threshold = 150f
    val flyOut = 900f
    val scope = rememberCoroutineScope()

    val displayX = if (dragging) rawX else animX.value
    val displayY = if (dragging) rawY else animY.value

    val dragProgress = (maxOf(abs(displayX), abs(displayY)) / threshold).coerceIn(0f, 1f)
    val nextScale = 0.92f + (0.08f * dragProgress)
    val tiltDeg = (displayX / threshold * 3f).coerceIn(-3f, 3f)

    Box(modifier) {
        // Next card (behind)
        if (nextCard != null) {
            Box(
                Modifier
                    .size(width, height)
                    .graphicsLayer {
                        scaleX = nextScale
                        scaleY = nextScale
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkCard)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                CardFace(nextCard.frontText, nextCard.frontImageUri)
            }
        }

        // Current card (on top)
        Box(
            modifier = Modifier
                .size(width, height)
                .offset { IntOffset(displayX.roundToInt(), displayY.roundToInt()) }
                .graphicsLayer {
                    rotationZ = tiltDeg
                    rotationY = flipProgress * flipSign
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(20.dp))
                .background(DarkCard)
                .pointerInput(card.id) {
                    detectTapGestures(
                        onTap = {
                            flipSign = if (Random.nextBoolean()) 1f else -1f
                            onTap()
                        }
                    )
                }
                .pointerInput(card.id) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            val x = rawX; val y = rawY

                            scope.launch {
                                when {
                                    y > threshold && abs(y) > abs(x) -> {
                                        animX.snapTo(x); animY.snapTo(y)
                                        animY.animateTo(flyOut, tween(220))
                                        onDown()
                                        animX.snapTo(0f); animY.snapTo(0f)
                                    }
                                    x > threshold -> {
                                        animX.snapTo(x); animY.snapTo(y)
                                        animX.animateTo(flyOut, tween(220))
                                        onRight()
                                        animX.snapTo(0f); animY.snapTo(0f)
                                    }
                                    x < -threshold -> {
                                        animX.snapTo(x); animY.snapTo(y)
                                        animX.animateTo(-flyOut, tween(220))
                                        onLeft()
                                        animX.snapTo(0f); animY.snapTo(0f)
                                    }
                                    else -> {
                                        animX.snapTo(x); animY.snapTo(y)
                                        launch { animX.animateTo(0f, tween(160)) }
                                        launch { animY.animateTo(0f, tween(160)) }
                                    }
                                }
                            }
                            rawX = 0f; rawY = 0f
                        },
                        onDragCancel = {
                            dragging = false
                            rawX = 0f; rawY = 0f
                            scope.launch {
                                launch { animX.animateTo(0f, tween(160)) }
                                launch { animY.animateTo(0f, tween(160)) }
                            }
                        }
                    ) { change, drag ->
                        change.consume()
                        rawX += drag.x
                        rawY += drag.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (flipProgress < 90f) {
                CardFace(card.frontText, card.frontImageUri, showHint = true)
            } else {
                Box(Modifier.graphicsLayer { rotationY = 180f }) {
                    CardFace(card.backText, card.backImageUri)
                }
            }

            // Colour overlay
            val overlay = when {
                displayX > 50 -> Correct
                displayX < -50 -> Wrong
                displayY > 50 -> Mastered
                else -> DarkCard
            }
            val alpha = when {
                abs(displayX) > 50 -> (abs(displayX) / threshold).coerceIn(0f, 0.35f)
                displayY > 50 -> (displayY / threshold).coerceIn(0f, 0.35f)
                else -> 0f
            }
            Box(Modifier.fillMaxSize().background(overlay.copy(alpha = alpha)))
        }
    }
}

@Composable
private fun EndCard(
    correct: Int,
    wrong: Int,
    removed: Int,
    width: Dp,
    height: Dp,
    onRepeat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val threshold = 220f
    val total = correct + wrong
    val pct = if (total > 0) correct * 100 / total else 0

    Box(
        modifier = modifier
            .size(width, height)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCard)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > threshold -> onRepeat()
                            offsetX < -threshold -> onClose()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f }
                ) { change, drag ->
                    change.consume()
                    offsetX += drag.x
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("RESULTS", style = MaterialTheme.typography.labelMedium, color = Accent)
            Spacer(Modifier.height(20.dp))
            Text("$pct%", color = AccentLight, style = MaterialTheme.typography.headlineLarge)
            Text("correct rate", color = Accent, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            val fraction = if (total > 0) correct.toFloat() / total else 0f
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = CorrectText,
                trackColor = WrongText.copy(alpha = 0.25f)
            )
            Spacer(Modifier.height(16.dp))
            Text("$correct correct  ·  $wrong wrong", color = Accent, style = MaterialTheme.typography.bodyMedium)
            if (removed > 0) {
                Spacer(Modifier.height(4.dp))
                Text("$removed removed from deck", color = CounterSoft, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("← Close", color = Accent, style = MaterialTheme.typography.labelSmall)
                Text("Repeat →", color = AccentLight, style = MaterialTheme.typography.labelSmall)
            }
        }

        val overlay = if (offsetX > 50) Correct else if (offsetX < -50) Wrong else DarkCard
        val alpha = if (abs(offsetX) > 50) (abs(offsetX) / threshold).coerceIn(0f, 0.35f) else 0f
        Box(Modifier.fillMaxSize().background(overlay.copy(alpha = alpha)))
    }
}

@Composable
private fun CardFace(text: String, imageUri: String?, showHint: Boolean = false) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = File(imageUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                if (text.isNotBlank()) Spacer(Modifier.height(12.dp))
            }
            if (text.isNotBlank()) {
                Text(text, color = AccentLight, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }
        if (showHint) {
            Text(
                "TAP TO FLIP",
                color = CounterSoft,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
            )
        }
    }
}
