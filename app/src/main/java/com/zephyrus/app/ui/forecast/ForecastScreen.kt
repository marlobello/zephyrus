package com.zephyrus.app.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyrus.app.domain.model.DailyForecast
import com.zephyrus.app.domain.model.TemperatureUnit
import com.zephyrus.app.ui.components.ZephyrusTopAppBar
import com.zephyrus.app.ui.theme.TempBlue
import com.zephyrus.app.ui.theme.TempDeepBlue
import com.zephyrus.app.ui.theme.TempDeepRed
import com.zephyrus.app.ui.theme.TempLightBlue
import com.zephyrus.app.ui.theme.TempLightRed
import com.zephyrus.app.ui.theme.TempRed
import com.zephyrus.app.ui.theme.TempWhite
import com.zephyrus.app.util.WeatherIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    locationName: String = "Zephyrus",
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: ForecastViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(latitude, longitude) {
        if (latitude != 0.0 || longitude != 0.0) {
            viewModel.loadForecast(latitude, longitude)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ZephyrusTopAppBar(
            locationName = locationName,
            subtitle = "10-Day Forecast",
            onRefreshClick = { viewModel.loadForecast(latitude, longitude) },
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
        )

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = uiState.error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.retry(latitude, longitude) }) { Text("Retry") }
                }
            }
            else -> {
                val forecasts = uiState.dailyForecast
                val overallMin = forecasts.minOfOrNull { it.temperatureMin } ?: 0.0
                val overallMax = forecasts.maxOfOrNull { it.temperatureMax } ?: 100.0

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    items(forecasts) { day ->
                        DailyForecastCard(day, uiState.temperatureUnit, overallMin, overallMax)
                    }
                }
            }
        }
    }
}

private val dateDisplayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

private fun formatForecastDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        val today = LocalDate.now()
        when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(dateDisplayFormatter)
        }
    } catch (_: Exception) {
        isoDate
    }
}

@Composable
private fun DailyForecastCard(
    day: DailyForecast,
    unit: TemperatureUnit,
    overallMin: Double,
    overallMax: Double,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Top row: date, icon, condition, details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatForecastDate(day.date),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(100.dp),
                )

                Icon(
                    imageVector = WeatherIcons.forCondition(day.condition),
                    contentDescription = day.condition.description,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = day.condition.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${day.precipitationProbability}% precip",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Wind ${day.windSpeedMax.toInt()} mph",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Temperature range bar
            TemperatureRangeBar(
                low = day.temperatureMin,
                high = day.temperatureMax,
                overallMin = overallMin,
                overallMax = overallMax,
                unit = unit,
            )
        }
    }
}

// Temperature color stops: (°F threshold, color)
private val tempColorStops = listOf(
    0f to TempDeepBlue,
    20f to TempBlue,
    40f to TempLightBlue,
    60f to TempWhite,
    75f to TempLightRed,
    90f to TempRed,
    105f to TempDeepRed,
)

private fun temperatureToColor(tempF: Float): Color {
    if (tempF <= tempColorStops.first().first) return tempColorStops.first().second
    if (tempF >= tempColorStops.last().first) return tempColorStops.last().second
    for (i in 0 until tempColorStops.size - 1) {
        val (lowTemp, lowColor) = tempColorStops[i]
        val (highTemp, highColor) = tempColorStops[i + 1]
        if (tempF in lowTemp..highTemp) {
            val fraction = ((tempF - lowTemp) / (highTemp - lowTemp)).coerceIn(0f, 1f)
            return lerp(lowColor, highColor, fraction)
        }
    }
    return tempColorStops.last().second
}

private fun toFahrenheit(temp: Double, unit: TemperatureUnit): Float = when (unit) {
    TemperatureUnit.FAHRENHEIT -> temp.toFloat()
    TemperatureUnit.CELSIUS -> (temp * 9.0 / 5.0 + 32.0).toFloat()
}

@Composable
private fun TemperatureRangeBar(
    low: Double,
    high: Double,
    overallMin: Double,
    overallMax: Double,
    unit: TemperatureUnit,
) {
    val totalRange = (overallMax - overallMin).coerceAtLeast(1.0)
    val startFraction = ((low - overallMin) / totalRange).toFloat().coerceIn(0f, 1f)
    val endFraction = ((high - overallMin) / totalRange).toFloat().coerceIn(0f, 1f)
    val barFraction = (endFraction - startFraction).coerceAtLeast(0.02f)

    val lowColorF = toFahrenheit(low, unit)
    val highColorF = toFahrenheit(high, unit)
    val startColor = temperatureToColor(lowColorF)
    val endColor = temperatureToColor(highColorF)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${low.toInt()}${unit.label()}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (startFraction > 0f) {
                    Spacer(modifier = Modifier.weight(startFraction))
                }
                Box(
                    modifier = Modifier
                        .weight(barFraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(listOf(startColor, endColor)),
                        ),
                )
                val endSpace = 1f - endFraction
                if (endSpace > 0f) {
                    Spacer(modifier = Modifier.weight(endSpace))
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${high.toInt()}${unit.label()}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(42.dp),
        )
    }
}
