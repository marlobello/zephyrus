package com.zephyrus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DAY_COLOR = Color(0xFFFFC107)
private val NIGHT_COLOR = Color(0xFF78909C)

/**
 * A card displaying sunrise and sunset with a full day/night elliptical arc.
 * The upper arc (above the horizon) represents daytime, the lower arc nighttime.
 * The sun's position is calculated using the viewed location's timezone.
 */
@Composable
fun SunArcCard(
    sunrise: String,
    sunset: String,
    timezone: String,
    clockFormat: ClockFormat,
    modifier: Modifier = Modifier,
) {
    val sunriseTime = parseSunTime(sunrise) ?: return
    val sunsetTime = parseSunTime(sunset) ?: return

    // Get current time in the viewed location's timezone
    val locationNow = try {
        ZonedDateTime.now(ZoneId.of(timezone)).toLocalTime()
    } catch (_: Exception) {
        LocalTime.now()
    }

    val sunriseLabel = formatSunTime(sunriseTime, clockFormat)
    val sunsetLabel = formatSunTime(sunsetTime, clockFormat)

    val isDaytime = locationNow.isAfter(sunriseTime) && locationNow.isBefore(sunsetTime)
    val sunPosition = calculateSunAngle(locationNow, sunriseTime, sunsetTime)

    val horizonColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
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
                    .height(120.dp),
            ) {
                drawSunArc(
                    sunAngle = sunPosition,
                    isDaytime = isDaytime,
                    horizonColor = horizonColor,
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
    sunAngle: Float,
    isDaytime: Boolean,
    horizonColor: Color,
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
    val horizonY = height * 0.45f
    val arcRadiusX = arcWidth / 2f
    val dayArcRadiusY = arcRadiusX * 0.35f
    val nightArcRadiusY = arcRadiusX * 0.22f

    // Draw dashed horizon line
    drawLine(
        color = horizonColor,
        start = Offset(arcLeft, horizonY),
        end = Offset(arcRight, horizonY),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
    )

    // Draw daytime arc (upper, left to right)
    val dayPath = Path()
    val steps = 60
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val angle = PI.toFloat() * (1f - t)
        val x = centerX + arcRadiusX * cos(angle)
        val y = horizonY - dayArcRadiusY * sin(angle)
        if (i == 0) dayPath.moveTo(x, y) else dayPath.lineTo(x, y)
    }
    drawPath(
        path = dayPath,
        color = DAY_COLOR.copy(alpha = 0.3f),
        style = Stroke(width = 3f, cap = StrokeCap.Round),
    )

    // Draw nighttime arc (lower, right to left)
    val nightPath = Path()
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val angle = -PI.toFloat() * t // 0 to -π (right to left, below horizon)
        val x = centerX + arcRadiusX * cos(angle)
        val y = horizonY - nightArcRadiusY * sin(angle) // sin is negative → below horizon
        if (i == 0) nightPath.moveTo(x, y) else nightPath.lineTo(x, y)
    }
    drawPath(
        path = nightPath,
        color = NIGHT_COLOR.copy(alpha = 0.2f),
        style = Stroke(width = 2f, cap = StrokeCap.Round),
    )

    // Draw traveled portion — solid arc up to current sun position
    val sunX: Float
    val sunY: Float
    val sunColor: Color

    if (sunAngle >= 0f) {
        // Daytime: angle is in [0, π]
        sunColor = DAY_COLOR
        sunX = centerX + arcRadiusX * cos(sunAngle)
        sunY = horizonY - dayArcRadiusY * sin(sunAngle)

        // Draw solid traveled day arc from sunrise (π) to current angle
        val traveledPath = Path()
        val travelSteps = ((PI.toFloat() - sunAngle) / PI.toFloat() * steps).toInt()
        for (i in 0..travelSteps) {
            val t = i.toFloat() / steps
            val a = PI.toFloat() * (1f - t)
            val x = centerX + arcRadiusX * cos(a)
            val y = horizonY - dayArcRadiusY * sin(a)
            if (i == 0) traveledPath.moveTo(x, y) else traveledPath.lineTo(x, y)
        }
        drawPath(
            path = traveledPath,
            color = DAY_COLOR,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
    } else {
        // Nighttime: angle is in [-π, 0]
        sunColor = NIGHT_COLOR
        sunX = centerX + arcRadiusX * cos(sunAngle)
        sunY = horizonY - nightArcRadiusY * sin(sunAngle)

        // Full day arc is traveled (solid)
        drawPath(
            path = dayPath,
            color = DAY_COLOR.copy(alpha = 0.5f),
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )

        // Draw solid traveled night arc from sunset (0) to current angle
        val nightProgress = -sunAngle / PI.toFloat()
        val travelSteps = (nightProgress * steps).toInt()
        val traveledNight = Path()
        for (i in 0..travelSteps) {
            val t = i.toFloat() / steps
            val a = -PI.toFloat() * t
            val x = centerX + arcRadiusX * cos(a)
            val y = horizonY - nightArcRadiusY * sin(a)
            if (i == 0) traveledNight.moveTo(x, y) else traveledNight.lineTo(x, y)
        }
        drawPath(
            path = traveledNight,
            color = NIGHT_COLOR,
            style = Stroke(width = 2f, cap = StrokeCap.Round),
        )
    }

    // Draw sun circle
    drawCircle(color = sunColor.copy(alpha = 0.2f), radius = 18f, center = Offset(sunX, sunY))
    drawCircle(color = sunColor, radius = 11f, center = Offset(sunX, sunY))
    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 5f, center = Offset(sunX, sunY))

    // Sunrise label (bottom-left of horizon)
    val sunriseResult = textMeasurer.measure(sunriseLabel, labelStyle)
    drawText(
        textLayoutResult = sunriseResult,
        topLeft = Offset(arcLeft, horizonY + 6f),
    )

    // Sunset label (bottom-right of horizon)
    val sunsetResult = textMeasurer.measure(sunsetLabel, labelStyle)
    drawText(
        textLayoutResult = sunsetResult,
        topLeft = Offset(arcRight - sunsetResult.size.width, horizonY + 6f),
    )
}

private fun parseSunTime(isoTime: String): LocalTime? {
    if (isoTime.isBlank()) return null
    return try {
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

/**
 * Calculate the sun's angle on the day/night ellipse.
 * Returns a value in [−π, π]:
 *   π = sunrise (left horizon), 0 = sunset (right horizon)
 *   Positive = daytime (above horizon), Negative = nighttime (below horizon)
 */
private fun calculateSunAngle(now: LocalTime, sunrise: LocalTime, sunset: LocalTime): Float {
    val nowSec = now.toSecondOfDay()
    val riseSec = sunrise.toSecondOfDay()
    val setSec = sunset.toSecondOfDay()
    val dayDuration = setSec - riseSec

    if (dayDuration <= 0) return 0f

    return when {
        // Daytime: map sunrise→sunset to angle π→0
        nowSec in riseSec..setSec -> {
            val progress = (nowSec - riseSec).toFloat() / dayDuration
            PI.toFloat() * (1f - progress)
        }
        // Night after sunset: map sunset→midnight→sunrise to angle 0→−π
        else -> {
            val nightDuration = 24 * 3600 - dayDuration
            val elapsed = if (nowSec >= setSec) {
                nowSec - setSec
            } else {
                (24 * 3600 - setSec) + nowSec
            }
            val nightProgress = elapsed.toFloat() / nightDuration
            -PI.toFloat() * nightProgress
        }
    }
}
