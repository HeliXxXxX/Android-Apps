package com.helix.budget.util

import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Money is stored everywhere as a [Long] count of minor units (cents) to avoid
 * floating-point rounding errors. These helpers convert to/from display strings.
 */
object Money {

    private val grouped = DecimalFormat("#,##0")
    private val groupedDecimals = DecimalFormat("#,##0.00")

    /**
     * Format minor units for display, prefixed with [symbol].
     * Whole amounts drop the decimals ("Rs 1,000"); fractional amounts keep them
     * ("Rs 1,000.50"). Sign is dropped — callers colour income/expense instead.
     */
    fun format(amountMinor: Long, symbol: String): String {
        val abs = abs(amountMinor)
        val whole = abs / 100
        val cents = abs % 100
        val number = if (cents == 0L) grouped.format(whole)
        else groupedDecimals.format(abs / 100.0)
        return "$symbol $number"
    }

    /** Like [format] but with an explicit leading +/− (for transaction rows). */
    fun formatSigned(amountMinor: Long, symbol: String): String {
        val sign = if (amountMinor < 0) "−" else "+"
        return "$sign ${format(amountMinor, symbol)}"
    }

    /**
     * Parse user keypad input into minor units. Accepts simple sums/differences
     * like "12 + 5" or "20-3.50". Returns null if nothing valid was entered.
     */
    fun parseExpression(input: String): Long? {
        val cleaned = input.replace(" ", "")
        if (cleaned.isBlank()) return null

        // Tokenise into signed terms. Leading sign optional.
        var total = 0.0
        var current = StringBuilder()
        var sign = 1
        var sawNumber = false

        fun flush(): Boolean {
            if (current.isEmpty()) return true // allow trailing operator
            val value = current.toString().toDoubleOrNull() ?: return false
            total += sign * value
            sawNumber = true
            current = StringBuilder()
            return true
        }

        for (ch in cleaned) {
            when (ch) {
                '+', '-' -> {
                    if (!flush()) return null
                    sign = if (ch == '-') -1 else 1
                }
                else -> current.append(ch)
            }
        }
        if (!flush()) return null
        if (!sawNumber) return null

        return (total * 100).roundToLong()
    }
}
