package com.zephyrus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
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
private const val SECONDS_IN_DAY = 24 * 3600

/**
 * A card displaying sunrise and sunset with a full day/night elliptical arc.
 * The ellipse shifts vertically based on the daylight-to-night ratio:
 * centered at equinox, shifted up in summer, down in winter.
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

    val locationNow = try {
        ZonedDateTime.now(ZoneId.of(timezone)).toLocalTime()
    } catch (_: Exception) {
        LocalTime.now()
    }

    val sunriseLabel = formatSunTime(sunriseTime, clockFormat)
    val sunsetLabel = formatSunTime(sunsetTime, clockFormat)
    val currentTimeLabel = formatSunTime(locationNow, clockFormat)

    val dayDuration = sunsetTime.toSecondOfDay() - sunriseTime.toSecondOfDay()
    val dayFraction = if (dayDuration > 0) dayDuration.toFloat() / SECONDS_IN_DAY else 0.5f

    val isDaytime = locationNow.isAfter(sunriseTime) && locationNow.isBefore(sunsetTime)
    val sunAngle = calculateSunAngle(locationNow, sunriseTime, sunsetTime)

    val horizonColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val titleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val titleStyle = TextStyle(fontSize = 11.sp, color = titleColor)
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)
    val textMeasurer = rememberTextMeasurer()

    Card(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(12.dp),
        ) {
            drawSunArc(
                sunAngle = sunAngle,
                dayFraction = dayFraction,
                isDaytime = isDaytime,
                horizonColor = horizonColor,
                sunriseLabel = sunriseLabel,
                sunsetLabel = sunsetLabel,
                currentTimeLabel = currentTimeLabel,
                titleStyle = titleStyle,
                labelStyle = labelStyle,
                textMeasurer = textMeasurer,
            )
        }
    }
}

private fun DrawScope.drawSunArc(
    sunAngle: Float,
    dayFraction: Float,
    isDaytime: Boolean,
    horizonColor: Color,
    sunriseLabel: String,
    sunsetLabel: String,
    currentTimeLabel: String,
    titleStyle: TextStyle,
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
    val arcRadiusX = arcWidth / 2f
    val arcRadiusY = arcRadiusX * 0.30f

    // Ellipse is always vertically centered in the canvas
    val ellipseCenterY = height * 0.46f

    // Horizon shifts based on daylight fraction:
    // equinox (0.5) → horizon at ellipse center; summer → horizon lower; winter → horizon higher
    val verticalOffset = arcRadiusY * (2f * dayFraction - 1f)
    val horizonY = ellipseCenterY + verticalOffset

    // Draw dashed horizon line
    drawLine(
        color = horizonColor,
        start = Offset(arcLeft, horizonY),
        end = Offset(arcRight, horizonY),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
    )

    // Draw title centered just above the horizon
    val titleResult = textMeasurer.measure("Sunrise & Sunset", titleStyle)
    drawText(
        textLayoutResult = titleResult,
        topLeft = Offset(
            centerX - titleResult.size.width / 2f,
            horizonY - titleResult.size.height - 4f,
        ),
    )

    // Helper to compute point on the ellipse at a given angle
    fun ellipsePoint(angle: Float): Offset {
        val x = centerX + arcRadiusX * cos(angle)
        val y = ellipseCenterY - arcRadiusY * sin(angle)
        return Offset(x, y)
    }

    // Draw full ellipse: day arc (upper) + night arc (lower)
    val steps = 60

    // Day arc: angle π → 0 (left to right, above center)
    val dayPath = Path()
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val angle = PI.toFloat() * (1f - t)
        val pt = ellipsePoint(angle)
        if (i == 0) dayPath.moveTo(pt.x, pt.y) else dayPath.lineTo(pt.x, pt.y)
    }
    drawPath(
        path = dayPath,
        color = DAY_COLOR.copy(alpha = 0.3f),
        style = Stroke(width = 3f, cap = StrokeCap.Round),
    )

    // Night arc: angle 0 → −π (right to left, below center)
    val nightPath = Path()
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val angle = -PI.toFloat() * t
        val pt = ellipsePoint(angle)
        if (i == 0) nightPath.moveTo(pt.x, pt.y) else nightPath.lineTo(pt.x, pt.y)
    }
    drawPath(
        path = nightPath,
        color = NIGHT_COLOR.copy(alpha = 0.2f),
        style = Stroke(width = 2f, cap = StrokeCap.Round),
    )

    // Draw traveled portion and position sun
    val sunColor: Color
    val sunPos: Offset

    if (sunAngle >= 0f) {
        // Daytime
        sunColor = DAY_COLOR
        sunPos = ellipsePoint(sunAngle)

        // Solid traveled day arc from sunrise (π) to current angle
        val traveledPath = Path()
        val travelSteps = ((PI.toFloat() - sunAngle) / PI.toFloat() * steps).toInt()
        for (i in 0..travelSteps) {
            val t = i.toFloat() / steps
            val a = PI.toFloat() * (1f - t)
            val pt = ellipsePoint(a)
            if (i == 0) traveledPath.moveTo(pt.x, pt.y) else traveledPath.lineTo(pt.x, pt.y)
        }
        drawPath(
            path = traveledPath,
            color = DAY_COLOR,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
    } else {
        // Nighttime
        sunColor = NIGHT_COLOR
        sunPos = ellipsePoint(sunAngle)

        // Full day arc shown as faded solid
        drawPath(
            path = dayPath,
            color = DAY_COLOR.copy(alpha = 0.5f),
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )

        // Solid traveled night arc from sunset (0) to current angle
        val nightProgress = -sunAngle / PI.toFloat()
        val travelSteps = (nightProgress * steps).toInt()
        val traveledNight = Path()
        for (i in 0..travelSteps) {
            val t = i.toFloat() / steps
            val a = -PI.toFloat() * t
            val pt = ellipsePoint(a)
            if (i == 0) traveledNight.moveTo(pt.x, pt.y) else traveledNight.lineTo(pt.x, pt.y)
        }
        drawPath(
            path = traveledNight,
            color = NIGHT_COLOR,
            style = Stroke(width = 2f, cap = StrokeCap.Round),
        )
    }

    // Sun circle
    drawCircle(color = sunColor.copy(alpha = 0.2f), radius = 18f, center = sunPos)
    drawCircle(color = sunColor, radius = 11f, center = sunPos)
    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 5f, center = sunPos)

    // Current time label near sun
    val timeResult = textMeasurer.measure(currentTimeLabel, labelStyle)
    val timeX = sunPos.x - timeResult.size.width / 2f
    // Place label above the sun if sun is in lower half, below if in upper half
    val timeY = if (sunPos.y > ellipseCenterY) {
        sunPos.y - 18f - timeResult.size.height
    } else {
        sunPos.y + 20f
    }
    drawText(
        textLayoutResult = timeResult,
        topLeft = Offset(timeX, timeY),
    )

    // Sunrise label (left of horizon)
    val sunriseResult = textMeasurer.measure(sunriseLabel, labelStyle)
    drawText(
        textLayoutResult = sunriseResult,
        topLeft = Offset(arcLeft, horizonY + 6f),
    )

    // Sunset label (right of horizon)
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
        nowSec in riseSec..setSec -> {
            val progress = (nowSec - riseSec).toFloat() / dayDuration
            PI.toFloat() * (1f - progress)
        }
        else -> {
            val nightDuration = SECONDS_IN_DAY - dayDuration
            val elapsed = if (nowSec >= setSec) {
                nowSec - setSec
            } else {
                (SECONDS_IN_DAY - setSec) + nowSec
            }
            val nightProgress = elapsed.toFloat() / nightDuration
            -PI.toFloat() * nightProgress
        }
    }
}
