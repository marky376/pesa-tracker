package com.example.mpesatracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mpesatracker.data.CategoryTotal
import com.example.mpesatracker.data.TransactionEntity
import com.example.mpesatracker.ui.formatDateTime
import com.example.mpesatracker.ui.formatKsh
import com.example.mpesatracker.ui.theme.IncomeGreen
import com.example.mpesatracker.ui.theme.color

/** One line in a list of transactions: icon, who, category and date, amount. */
@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val categoryColor = transaction.category.color
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = transaction.category.label.take(1),
                color = categoryColor,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.counterparty,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${transaction.category.label} · ${formatDateTime(transaction.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        val isIncome = transaction.type.isIncome
        Text(
            text = (if (isIncome) "+" else "-") + formatKsh(transaction.amount),
            color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A donut chart of spending per category, drawn by hand on a Canvas. */
@Composable
fun SpendingDonutChart(data: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.total }
    Canvas(modifier = modifier) {
        if (total <= 0.0) return@Canvas
        val strokeWidth = size.minDimension * 0.18f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var startAngle = -90f // start at 12 o'clock
        data.forEach { slice ->
            val sweep = (slice.total / total * 360.0).toFloat()
            drawArc(
                color = slice.category.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth),
            )
            startAngle += sweep
        }
    }
}

/** The legend next to the chart: colour dot, category, amount and share. */
@Composable
fun CategoryLegend(data: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.total }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        data.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(item.category.color)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(item.category.label, style = MaterialTheme.typography.bodyMedium)
                    val percent = if (total > 0) (item.total / total * 100).toInt() else 0
                    Text(
                        "${formatKsh(item.total)} · $percent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A row of chips that scrolls sideways when it doesn't fit. */
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
