package com.example.steppet.ui.screen.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun StepHistoryScreen() {
    val stepData = listOf(10, 20, 30, 40, 50, 60, 70)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Daily Step History",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(32.dp))

        BarChart(values = stepData)
    }
}

@Composable
fun BarChart(values: List<Int>) {
    val maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: 1
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
    ) {
        val barWidth = size.width / (values.size * 2)
        val barSpacing = barWidth

        values.forEachIndexed { index, value ->
            val left = index * (barWidth + barSpacing) + barSpacing / 2
            val top = size.height - (value / maxValue.toFloat()) * size.height
            val height = size.height - top

            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

