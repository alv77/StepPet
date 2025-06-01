package com.example.steppet.ui.screen.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun StepHistoryScreen() {
    val stepData = listOf(10, 20, 30, 40, 50, 60, 70)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Daily Step History",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(32.dp))

        BarChart(
            values = stepData,
            labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        )
    }
}

@Composable
fun BarChart(values: List<Int>, labels: List<String>) {
    val maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onBackground
    val highlightColor = MaterialTheme.colorScheme.tertiary

    var selectedIndex by remember { mutableStateOf(-1) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp)
    ) {
        val density = LocalDensity.current
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val barWidth = totalWidthPx / (values.size * 2f)
        val barSpacing = barWidth

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(values) {
                    detectTapGestures { tapOffset ->
                        values.forEachIndexed { index, _ ->
                            val xStart = index * (barWidth + barSpacing) + barSpacing / 2f
                            val xEnd = xStart + barWidth
                            if (tapOffset.x in xStart..xEnd) {
                                selectedIndex = index
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            values.forEachIndexed { index, value ->
                val x = index * (barWidth + barSpacing) + barSpacing / 2f
                val barHeight = (value / maxValue.toFloat()) * (size.height - 50f)
                val isSelected = index == selectedIndex

                // Value above bar
                if (isSelected) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "$value",
                        x + barWidth / 2f,
                        size.height - barHeight - 40f,
                        android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = 14.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                // Bar
                drawRoundRect(
                    color = if (isSelected) highlightColor else barColor,
                    topLeft = Offset(x, size.height - barHeight - 20f),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Label below bar
                drawContext.canvas.nativeCanvas.drawText(
                    labels.getOrNull(index) ?: "",
                    x + barWidth / 2f,
                    size.height + 16f,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}
