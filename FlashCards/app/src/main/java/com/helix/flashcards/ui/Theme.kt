package com.helix.flashcards.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Surfaces ──
val DarkBg = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1A1A1A)
val DarkCard = Color(0xFF242424)
val Accent = Color(0xFFB0B0B0)
val AccentLight = Color(0xFFE0E0E0)

// ── Swipe feedback on the card (kept as colour cue) ──
val Correct = Color(0xFF2E7D32)
val Wrong = Color(0xFFC62828)
val Mastered = Color(0xFF1565C0)

// ── Text-readable semantic colours ──
val CorrectText = Color(0xFF5CB85C)
val WrongText = Color(0xFFEF5350)

// ── Counter shades (monochrome, three tones) ──
val CounterStrong = Color(0xFFE6E6E6)
val CounterMid = Color(0xFFADADAD)
val CounterSoft = Color(0xFF7A7A7A)

// ── Accent used to frame a deck page (translucent) ──
val DeckAccent = Color(0xFF7C5CBF)

private val DarkColors = darkColorScheme(
    primary = AccentLight,
    onPrimary = DarkBg,
    secondary = Accent,
    onSecondary = DarkBg,
    background = DarkBg,
    onBackground = AccentLight,
    surface = DarkSurface,
    onSurface = AccentLight,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Accent,
    outline = Color(0xFF3A3A3A),
    error = Wrong,
)

private val sans = FontFamily.SansSerif

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.4.sp),
)

@Composable
fun FlashCardsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}