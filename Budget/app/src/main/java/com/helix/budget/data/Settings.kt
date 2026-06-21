package com.helix.budget.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.util.Currency
import java.util.Locale

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

/**
 * Reactive settings backed by SharedPreferences. Reads are Compose-observable
 * via [mutableStateOf]; writes persist immediately.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("budget_settings", Context.MODE_PRIVATE)

    // ── Accent colour ──
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

    // ── Currency symbol ──
    var currencySymbol by mutableStateOf(
        prefs.getString(KEY_CURRENCY, null) ?: defaultCurrencySymbol()
    )
        private set

    fun updateCurrency(symbol: String) {
        val s = symbol.trim().ifBlank { "$" }
        currencySymbol = s
        prefs.edit().putString(KEY_CURRENCY, s).apply()
    }

    // ── Active budget period ──
    var periodActive by mutableStateOf(prefs.getBoolean(KEY_PERIOD_ACTIVE, false))
        private set

    /** Total length of the period in days (e.g. 14 for two weeks). */
    var periodLengthDays by mutableIntStateOf(prefs.getInt(KEY_PERIOD_DAYS, 14))
        private set

    /** Day the period started, as epoch-day (System.currentTimeMillis()/86_400_000). */
    var periodStartEpochDay by mutableStateOf(prefs.getLong(KEY_PERIOD_START, todayEpochDay()))
        private set

    fun setPeriod(lengthDays: Int) {
        periodActive = true
        periodLengthDays = lengthDays.coerceAtLeast(1)
        periodStartEpochDay = todayEpochDay()
        prefs.edit()
            .putBoolean(KEY_PERIOD_ACTIVE, true)
            .putInt(KEY_PERIOD_DAYS, periodLengthDays)
            .putLong(KEY_PERIOD_START, periodStartEpochDay)
            .apply()
    }

    fun clearPeriod() {
        periodActive = false
        prefs.edit().putBoolean(KEY_PERIOD_ACTIVE, false).apply()
    }

    /** Whole days already elapsed since the period started (0 on the start day). */
    fun daysElapsed(): Int = (todayEpochDay() - periodStartEpochDay).toInt().coerceAtLeast(0)

    /** Days remaining in the period, never less than 1 so we never divide by zero. */
    fun daysLeft(): Int = (periodLengthDays - daysElapsed()).coerceAtLeast(1)

    companion object {
        private const val KEY_ACCENT = "accent_index"
        private const val KEY_CURRENCY = "currency_symbol"
        private const val KEY_PERIOD_ACTIVE = "period_active"
        private const val KEY_PERIOD_DAYS = "period_days"
        private const val KEY_PERIOD_START = "period_start_epoch_day"

        private fun todayEpochDay(): Long = System.currentTimeMillis() / 86_400_000L

        private fun defaultCurrencySymbol(): String =
            runCatching { Currency.getInstance(Locale.getDefault()).symbol }.getOrNull() ?: "$"
    }
}
