package com.helix.flashcards.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class AccentOption(val name: String, val color: Color)

// Muted, dark-friendly accents (read well as a translucent panel)
val AccentOptions = listOf(
    AccentOption("Violet", Color(0xFF7C5CBF)),
    AccentOption("Indigo", Color(0xFF5C6BC0)),
    AccentOption("Teal", Color(0xFF3E8E8A)),
    AccentOption("Forest", Color(0xFF4E7C59)),
    AccentOption("Crimson", Color(0xFFB05C6F)),
    AccentOption("Amber", Color(0xFFB08D57)),
    AccentOption("Slate", Color(0xFF6B7280)),
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("flashcards_settings", Context.MODE_PRIVATE)

    var accentIndex by mutableIntStateOf(
        prefs.getInt(KEY_ACCENT, 0).coerceIn(0, AccentOptions.lastIndex)
    )
        private set

    val accent: Color get() = AccentOptions[accentIndex].color

    fun setAccent(i: Int) {
        if (i in AccentOptions.indices) {
            accentIndex = i
            prefs.edit().putInt(KEY_ACCENT, i).apply()
        }
    }

    companion object {
        private const val KEY_ACCENT = "accent_index"
    }
}