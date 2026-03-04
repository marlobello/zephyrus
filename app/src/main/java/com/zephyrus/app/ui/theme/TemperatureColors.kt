package com.zephyrus.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.zephyrus.app.domain.model.TemperatureUnit

// Temperature color stops: (°F threshold, color)
val tempColorStops = listOf(
    0f to TempDeepBlue,
    20f to TempBlue,
    40f to TempLightBlue,
    60f to TempWhite,
    75f to TempLightRed,
    90f to TempRed,
    105f to TempDeepRed,
)

// Humidity color stops: (% threshold, color) — dry tan → green → deep blue
val HumidityDry = Color(0xFFD2B48C)       // 0% - tan/dry
val HumidityLow = Color(0xFFA8D08D)       // 25% - light green
val HumidityMedium = Color(0xFF4CAF50)    // 50% - green
val HumidityHigh = Color(0xFF42A5F5)      // 75% - blue
val HumiditySaturated = Color(0xFF0D47A1) // 100% - deep blue

val humidityColorStops = listOf(
    0f to HumidityDry,
    25f to HumidityLow,
    50f to HumidityMedium,
    75f to HumidityHigh,
    100f to HumiditySaturated,
)

// Pressure color stops: (hPa threshold, color) — low purple → green → high orange
// Typical surface pressure range: ~980–1040 hPa
val PressureLow = Color(0xFF7B1FA2)       // 980 hPa - deep purple (storm)
val PressureBelowAvg = Color(0xFF5C6BC0)  // 1000 hPa - indigo
val PressureNormal = Color(0xFF66BB6A)    // 1013 hPa - green (standard)
val PressureAboveAvg = Color(0xFFFFA726)  // 1025 hPa - orange
val PressureHigh = Color(0xFFD32F2F)      // 1040 hPa - red (high pressure)

val pressureColorStops = listOf(
    980f to PressureLow,
    1000f to PressureBelowAvg,
    1013f to PressureNormal,
    1025f to PressureAboveAvg,
    1040f to PressureHigh,
)

// Precipitation color stops: (inch/h threshold, color) — clear → heavy rain
val PrecipNone = Color(0xFFE8F5E9)         // 0.0 in/h - very light green (dry)
val PrecipLight = Color(0xFF90CAF9)        // 0.04 in/h - light blue
val PrecipModerate = Color(0xFF42A5F5)     // 0.1 in/h - blue
val PrecipHeavy = Color(0xFF1565C0)        // 0.3 in/h - dark blue
val PrecipExtreme = Color(0xFF7B1FA2)      // 0.5+ in/h - purple

val precipColorStops = listOf(
    0f to PrecipNone,
    0.04f to PrecipLight,
    0.1f to PrecipModerate,
    0.3f to PrecipHeavy,
    0.5f to PrecipExtreme,
)

fun temperatureToColor(tempF: Float): Color {
    return interpolateColorStops(tempColorStops, tempF)
}

fun humidityToColor(humidity: Float): Color {
    return interpolateColorStops(humidityColorStops, humidity)
}

fun pressureToColor(pressureHpa: Float): Color {
    return interpolateColorStops(pressureColorStops, pressureHpa)
}

fun precipitationToColor(inchPerHour: Float): Color {
    return interpolateColorStops(precipColorStops, inchPerHour)
}

private fun interpolateColorStops(stops: List<Pair<Float, Color>>, value: Float): Color {
    if (value <= stops.first().first) return stops.first().second
    if (value >= stops.last().first) return stops.last().second
    for (i in 0 until stops.size - 1) {
        val (low, lowColor) = stops[i]
        val (high, highColor) = stops[i + 1]
        if (value in low..high) {
            val fraction = ((value - low) / (high - low)).coerceIn(0f, 1f)
            return lerp(lowColor, highColor, fraction)
        }
    }
    return stops.last().second
}

fun toFahrenheit(temp: Double, unit: TemperatureUnit): Float = when (unit) {
    TemperatureUnit.FAHRENHEIT -> temp.toFloat()
    TemperatureUnit.CELSIUS -> (temp * 9.0 / 5.0 + 32.0).toFloat()
}

fun temperatureToArgb(tempF: Float, alpha: Int = 160): Int {
    return colorToArgb(temperatureToColor(tempF), alpha)
}

fun humidityToArgb(humidity: Float, alpha: Int = 160): Int {
    return colorToArgb(humidityToColor(humidity), alpha)
}

fun pressureToArgb(pressureHpa: Float, alpha: Int = 160): Int {
    return colorToArgb(pressureToColor(pressureHpa), alpha)
}

fun precipitationToArgb(inchPerHour: Float, alpha: Int = 160): Int {
    return colorToArgb(precipitationToColor(inchPerHour), alpha)
}

private fun colorToArgb(color: Color, alpha: Int): Int {
    return android.graphics.Color.argb(
        alpha,
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
    )
}
