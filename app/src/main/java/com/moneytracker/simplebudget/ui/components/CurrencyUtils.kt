package com.moneytracker.simplebudget.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

private fun getAppDecimalSymbols(): DecimalFormatSymbols {
    val symbols = DecimalFormatSymbols(Locale.getDefault())
    symbols.groupingSeparator = '.'
    symbols.decimalSeparator = ','
    return symbols
}

/**
 * Formats a currency amount with the currency symbol.
 * Always uses dot for thousands and comma for decimals (e.g. 150.000,00).
 */
fun formatCurrency(amount: Double, currencyCode: String, symbolAfter: Boolean = true): String {
    return try {
        val symbol = getCurrencySymbol(currencyCode)
        val symbols = getAppDecimalSymbols()
        val format = DecimalFormat("#,##0.00", symbols)
        val formatted = format.format(amount)
        if (symbolAfter) "$formatted $symbol" else "$symbol$formatted"
    } catch (e: Exception) {
        val symbol = getCurrencySymbol(currencyCode)
        val formatted = String.format("%.2f", amount)
        if (symbolAfter) "$formatted $symbol" else "$symbol$formatted"
    }
}

/**
 * Formats a number without the currency symbol.
 * Always uses dot for thousands and comma for decimals (e.g. 150.000,00).
 */
fun formatNumber(amount: Double, currencyCode: String): String {
    return try {
        val symbols = getAppDecimalSymbols()
        val format = DecimalFormat("#,##0.00", symbols)
        format.format(amount)
    } catch (e: Exception) {
        String.format("%.2f", amount)
    }
}

/**
 * Returns the currency symbol (e.g. "$", "€", "£") for a given currency code,
 * using the currency's native locale so it doesn't get country-prefixed.
 */
fun getCurrencySymbol(currencyCode: String): String {
    return try {
        val locale = findLocaleForCurrency(currencyCode)
        Currency.getInstance(currencyCode).getSymbol(locale)
    } catch (e: Exception) {
        currencyCode
    }
}

private val localeCache = mutableMapOf<String, Locale>()

private fun findLocaleForCurrency(currencyCode: String): Locale {
    return localeCache.getOrPut(currencyCode) {
        // Find a locale whose default currency matches, so formatting is native
        Locale.getAvailableLocales().firstOrNull { locale ->
            try {
                Currency.getInstance(locale)?.currencyCode == currencyCode
            } catch (e: Exception) {
                false
            }
        } ?: Locale.getDefault()
    }
}

/**
 * Formats a raw amount input string with proper separators while preserving
 * the exact digits the user has typed (no rounding or trailing zero addition).
 * Uses dot for thousands and comma for decimals (e.g. "200000.00" -> "200.000,00").
 */
fun formatAmountInput(raw: String): String {
    if (raw.isEmpty()) return "0"

    val negative = raw.startsWith("-")
    val abs = if (negative) raw.substring(1) else raw

    val parts = abs.split(".")
    val intPart = parts[0].ifEmpty { "0" }
    val decPart = if (parts.size > 1) parts[1] else null

    // Format integer part with dot as thousands separator
    val formatted = intPart.reversed().chunked(3).joinToString(".").reversed()

    val result = if (decPart != null) {
        "$formatted,$decPart"
    } else {
        formatted
    }

    return if (negative) "-$result" else result
}

/**
 * Displays a currency amount with the symbol in smaller font aligned to the
 * top of the amount text, matching the transaction screen hero style.
 */
@Composable
fun CurrencyAmountText(
    amount: Double,
    currencyCode: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    symbolAfter: Boolean = true
) {
    val symbol = getCurrencySymbol(currencyCode)
    val formatted = formatNumber(amount, currencyCode)
    val effectiveStyle = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style
    val currencyStyle = effectiveStyle.copy(
        fontSize = effectiveStyle.fontSize * 0.75f,
        fontWeight = FontWeight.Medium
    )

    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        if (!symbolAfter) {
            Text(
                text = symbol,
                style = currencyStyle,
                color = color,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(1.dp))
        }
        Text(
            text = formatted,
            style = effectiveStyle,
            color = color,
            maxLines = maxLines,
            overflow = overflow
        )
        if (symbolAfter) {
            Spacer(modifier = Modifier.width(1.dp))
            Text(
                text = symbol,
                style = currencyStyle,
                color = color,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
