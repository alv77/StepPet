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
import com.example.steppet.data.cloud.CloudRepository
import java.time.LocalDate
import kotlin.math.roundToInt
import com.example.steppet.data.model.StepStats
import java.time.format.DateTimeFormatter


@Composable
fun StepHistoryScreen() {
    var stepMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var stats by remember { mutableStateOf<StepStats?>(null) }

    LaunchedEffect(Unit) {
        stepMap = CloudRepository.getStepsLast7Days().toSortedMap(compareBy { LocalDate.parse(it) })
        stats = CloudRepository.getGlobalStepStats()

    }

    val steps = stepMap.values.toList()
    val labels = stepMap.keys.map {
        LocalDate.parse(it).dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val textColor = MaterialTheme.colorScheme.onBackground
        Text("Daily Step History", style = MaterialTheme.typography.headlineMedium, color = textColor)

        Spacer(Modifier.height(32.dp))

        if (steps.isNotEmpty()) {
            BarChart(values = steps, labels = labels)

            Spacer(Modifier.height(128.dp))

            stats?.let {
                val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM") // e.g. Mon, 03 Jun

                val bestDay = it.bestDay?.let { LocalDate.parse(it).format(formatter) } ?: "N/A"
                val worstDay = it.worstDay?.let { LocalDate.parse(it).format(formatter) } ?: "N/A"


                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Text("Total steps: ${it.totalSteps}", color = textColor)
                    Text("Average: ${it.averageSteps} steps/day", color = textColor)
                    Text("Best: $bestDay — ${it.bestCount} steps", color = textColor)
                    Text("Worst: $worstDay — ${it.worstCount} steps", color = textColor)
                    Text("Current streak: ${it.currentStreak} day(s)", color = textColor)

                }
            } ?: CircularProgressIndicator()
        } else {
            CircularProgressIndicator()
        }
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
        val minBarHeightPx = with(density) { 6.dp.toPx() }
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
                var barHeight = (value / maxValue.toFloat()) * (size.height - 50f)
                barHeight = maxOf(barHeight, minBarHeightPx) // 🔥 ensure minimal height
                val isSelected = index == selectedIndex

                // Value above bar
                if (isSelected && value > 0) {
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
