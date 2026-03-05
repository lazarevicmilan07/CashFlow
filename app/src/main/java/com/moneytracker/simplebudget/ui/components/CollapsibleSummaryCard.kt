package com.moneytracker.simplebudget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.simplebudget.ui.theme.ExpenseRed
import com.moneytracker.simplebudget.ui.theme.IncomeGreen

@Composable
fun rememberCollapseProgress(listState: LazyListState): Float {
    val progress by remember {
        derivedStateOf {
            val threshold = 200f
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                else -> (listState.firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
            }
        }
    }
    return progress
}

private fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp {
    return start + (end - start) * fraction
}

private fun lerpSp(start: TextUnit, end: TextUnit, fraction: Float): TextUnit {
    return (start.value + (end.value - start.value) * fraction).sp
}

@Composable
fun CollapsibleSummaryCard(
    income: Double,
    expense: Double,
    balance: Double,
    currency: String,
    collapseProgress: Float,
    balanceLabel: String = "Balance",
    modifier: Modifier = Modifier,
    symbolAfter: Boolean = true
) {
    val p = collapseProgress

    val balanceColor = when {
        balance > 0 -> IncomeGreen
        balance < 0 -> ExpenseRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    val balanceLabelAlpha = (1f - p * 2f).coerceIn(0f, 1f)
    val balanceFontSize = lerpSp(28.sp, 18.sp, p)
    val balanceVerticalPadding = lerpDp(4.dp, 1.dp, p)
    val spacerAfterBalance = lerpDp(8.dp, 2.dp, p)
    val iconCircleSize = lerpDp(32.dp, 24.dp, p)
    val innerIconSize = lerpDp(16.dp, 12.dp, p)
    val cardPadding = lerpDp(10.dp, 6.dp, p)
    val incomeExpenseLabelAlpha = (1f - p * 1.5f).coerceIn(0f, 1f)
    val labelHeight = lerpDp(16.dp, 0.dp, p)
    val bottomSpacer = lerpDp(8.dp, 8.dp, p)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        // Balance hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = balanceVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .height(lerpDp(18.dp, 0.dp, p))
                    .graphicsLayer { alpha = balanceLabelAlpha }
            )
            CurrencyAmountText(
                amount = balance,
                currencyCode = currency,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = balanceFontSize),
                fontWeight = FontWeight.Bold,
                color = balanceColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                symbolAfter = symbolAfter
            )
        }

        Spacer(modifier = Modifier.height(spacerAfterBalance))

        // Income & Expense side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income card
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = IncomeGreen.copy(alpha = 0.1f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = IncomeGreen.copy(alpha = 0.2f),
                        modifier = Modifier.size(iconCircleSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(innerIconSize)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .height(labelHeight)
                                .graphicsLayer { alpha = incomeExpenseLabelAlpha }
                        )
                        CurrencyAmountText(
                            amount = income,
                            currencyCode = currency,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = IncomeGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            symbolAfter = symbolAfter
                        )
                    }
                }
            }

            // Expense card
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = ExpenseRed.copy(alpha = 0.1f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ExpenseRed.copy(alpha = 0.2f),
                        modifier = Modifier.size(iconCircleSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(innerIconSize)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .height(labelHeight)
                                .graphicsLayer { alpha = incomeExpenseLabelAlpha }
                        )
                        CurrencyAmountText(
                            amount = expense,
                            currencyCode = currency,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            symbolAfter = symbolAfter
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(bottomSpacer))
    }
}
