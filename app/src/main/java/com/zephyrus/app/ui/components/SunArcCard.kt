package com.zephyrus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zephyrus.app.domain.model.ClockFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A card displaying sunrise and sunset times with a semicircular sun arc.
 * The sun's position along the arc indicates the current time of day.
 * Before sunrise and after sunset, the sun sits at the respective endpoint.
 */
@Composable
fun SunArcCard(
    sunrise: String,
    sunset: String,
    clockFormat: ClockFormat,
    modifier: Modifier = Modifier,
) {
    val sunriseTime = parseSunTime(sunrise) ?: return
    val sunsetTime = parseSunTime(sunset) ?: return
    val now = LocalTime.now()

    val sunriseLabel = formatSunTime(sunriseTime, clockFormat)
    val sunsetLabel = formatSunTime(sunsetTime, clockFormat)

    // Progress: 0 = sunrise, 1 = sunset
    val progress = calculateSunProgress(now, sunriseTime, sunsetTime)
    val isDaytime = now.isAfter(sunriseTime) && now.isBefore(sunsetTime)

    val arcColor = if (isDaytime) {
        Color(0xFFFFC107) // amber/golden
    } else {
        Color(0xFF78909C) // blue-grey for night
    }
    val horizonColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val sunColor = if (isDaytime) Color(0xFFFFC107) else Color(0xFF78909C)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)
    val textMeasurer = rememberTextMeasurer()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Sunrise & Sunset",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            ) {
                drawSunArc(
                    progress = progress,
                    arcColor = arcColor,
                    horizonColor = horizonColor,
                    sunColor = sunColor,
                    sunriseLabel = sunriseLabel,
                    sunsetLabel = sunsetLabel,
                    labelStyle = labelStyle,
                    textMeasurer = textMeasurer,
                )
            }
        }
    }
}

private fun DrawScope.drawSunArc(
    progress: Float,
    arcColor: Color,
    horizonColor: Color,
    sunColor: Color,
    sunriseLabel: String,
    sunsetLabel: String,
    labelStyle: TextStyle,
    textMeasurer: TextMeasurer,
) {
    val width = size.width
    val height = size.height

    val hPadding = 24f
    val arcLeft = hPadding
    val arcRight = width - hPadding
    val arcWidth = arcRight - arcLeft
    val centerX = arcLeft + arcWidth / 2f
    // Horizon sits about 70% down the canvas to leave room for the arc above
    val horizonY = height * 0.72f
    val arcRadiusX = arcWidth / 2f
    // Flatten the arc to ~60% of a full semicircle for a more compact look
    val arcRadiusY = arcRadiusX * 0.38f

    // Draw dashed horizon line
    drawLine(
        color = horizonColor,
        start = Offset(arcLeft, horizonY),
        end = Offset(arcRight, horizonY),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
    )

    // Draw the semicircular arc (sunrise left to sunset right)
    val arcPath = Path()
    val steps = 60
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val angle = PI.toFloat() * (1f - t) // PI (left) to 0 (right)
        val x = centerX + arcRadiusX * cos(angle)
        val y = horizonY - arcRadiusY * sin(angle)
        if (i == 0) arcPath.moveTo(x, y) else arcPath.lineTo(x, y)
    }
    drawPath(
        path = arcPath,
        color = arcColor.copy(alpha = 0.4f),
        style = Stroke(width = 3f, cap = StrokeCap.Round),
    )

    // Draw the "traveled" portion of the arc (solid) up to current sun position
    if (progress > 0f) {
        val traveledPath = Path()
        val travelSteps = (steps * progress.coerceIn(0f, 1f)).toInt()
        for (i in 0..travelSteps) {
            val t = i.toFloat() / steps
            val angle = PI.toFloat() * (1f - t)
            val x = centerX + arcRadiusX * cos(angle)
            val y = horizonY - arcRadiusY * sin(angle)
            if (i == 0) traveledPath.moveTo(x, y) else traveledPath.lineTo(x, y)
        }
        drawPath(
            path = traveledPath,
            color = arcColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
    }

    // Draw sun circle at current position
    val sunT = progress.coerceIn(0f, 1f)
    val sunAngle = PI.toFloat() * (1f - sunT)
    val sunX = centerX + arcRadiusX * cos(sunAngle)
    val sunY = horizonY - arcRadiusY * sin(sunAngle)

    // Outer glow
    drawCircle(color = sunColor.copy(alpha = 0.2f), radius = 18f, center = Offset(sunX, sunY))
    // Sun dot
    drawCircle(color = sunColor, radius = 11f, center = Offset(sunX, sunY))
    // Inner highlight
    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 5f, center = Offset(sunX, sunY))

    // Draw sunrise label (bottom-left)
    val sunriseResult = textMeasurer.measure(sunriseLabel, labelStyle)
    drawText(
        textLayoutResult = sunriseResult,
        topLeft = Offset(arcLeft, horizonY + 6f),
    )

    // Draw sunset label (bottom-right)
    val sunsetResult = textMeasurer.measure(sunsetLabel, labelStyle)
    drawText(
        textLayoutResult = sunsetResult,
        topLeft = Offset(arcRight - sunsetResult.size.width, horizonY + 6f),
    )
}

private fun parseSunTime(isoTime: String): LocalTime? {
    if (isoTime.isBlank()) return null
    return try {
        // Open-Meteo returns "2024-03-04T06:23" format
        LocalDateTime.parse(isoTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalTime()
    } catch (_: Exception) {
        null
    }
}

private fun formatSunTime(time: LocalTime, clockFormat: ClockFormat): String {
    val formatter = if (clockFormat == ClockFormat.TWELVE_HOUR) {
        DateTimeFormatter.ofPattern("h:mm a")
    } else {
        DateTimeFormatter.ofPattern("HH:mm")
    }
    return time.format(formatter)
}

private fun calculateSunProgress(now: LocalTime, sunrise: LocalTime, sunset: LocalTime): Float {
    if (now.isBefore(sunrise)) return 0f
    if (now.isAfter(sunset)) return 1f
    val totalMinutes = (sunset.toSecondOfDay() - sunrise.toSecondOfDay()).toFloat()
    if (totalMinutes <= 0) return 0.5f
    val elapsed = (now.toSecondOfDay() - sunrise.toSecondOfDay()).toFloat()
    return (elapsed / totalMinutes).coerceIn(0f, 1f)
}
