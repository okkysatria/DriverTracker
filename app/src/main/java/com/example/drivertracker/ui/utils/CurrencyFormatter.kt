package com.example.drivertracker.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val indonesianSymbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

private val rupiahFormatter = DecimalFormat("#,##0", indonesianSymbols)

fun Long.toRupiahString(includeSymbol: Boolean = true): String {
    val absValue = kotlin.math.abs(this)
    val formatted = rupiahFormatter.format(absValue)
    val prefix = if (includeSymbol) "Rp " else ""
    return if (this < 0) "-$prefix$formatted" else "$prefix$formatted"
}

fun Double.toRupiahString(includeSymbol: Boolean = true): String {
    if (this.isNaN() || this.isInfinite()) {
        return if (includeSymbol) "Rp 0" else "0"
    }
    return kotlin.math.round(this).toLong().toRupiahString(includeSymbol)
}

fun String.parseRupiahToDouble(): Double {
    if (this.isBlank()) return 0.0
    val isNegative = this.contains("-")
    var clean = this.replace("Rp", "", ignoreCase = true)
        .replace("rp", "", ignoreCase = true)
        .replace("-", "")
        .trim()

    if (clean.isBlank()) return 0.0

    if (clean.contains(",") && clean.contains(".")) {
        clean = clean.replace(".", "").replace(",", ".")
    } else if (clean.contains(",")) {
        clean = clean.replace(",", ".")
    } else if (clean.contains(".")) {
        val parts = clean.split(".")
        if (parts.size > 2 || (parts.size == 2 && parts[1].length == 3)) {
            clean = clean.replace(".", "")
        }
    }

    val filtered = clean.filter { it.isDigit() || it == '.' }
    val result = filtered.toDoubleOrNull() ?: 0.0
    return if (isNegative) -result else result
}

class RupiahVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val formatted = StringBuilder()
        val len = digits.length
        for (i in 0 until len) {
            if (i > 0 && (len - i) % 3 == 0) {
                formatted.append('.')
            }
            formatted.append(digits[i])
        }

        val transformedString = formatted.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, digits.length)
                var dotsBefore = 0
                for (i in 1..clamped) {
                    if ((len - (i - 1)) % 3 == 0 && (i - 1) > 0) {
                        dotsBefore++
                    }
                }
                return (clamped + dotsBefore).coerceIn(0, transformedString.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformedString.length)
                var dotsBefore = 0
                for (i in 0 until clamped) {
                    if (transformedString[i] == '.') {
                        dotsBefore++
                    }
                }
                return (clamped - dotsBefore).coerceIn(0, digits.length)
            }
        }

        return TransformedText(AnnotatedString(transformedString), offsetMapping)
    }
}
