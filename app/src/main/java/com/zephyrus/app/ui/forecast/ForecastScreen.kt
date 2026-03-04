package com.zephyrus.app.ui.forecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyrus.app.domain.model.ClockFormat
import com.zephyrus.app.domain.model.DailyForecast
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.TemperatureUnit
import com.zephyrus.app.ui.components.HourlyForecastRow
import com.zephyrus.app.ui.components.ZephyrusTopAppBar
import com.zephyrus.app.ui.theme.temperatureToColor
import com.zephyrus.app.ui.theme.toFahrenheit
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
    onAboutClick: () -> Unit = {},
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
            onAboutClick = onAboutClick,
        )

        when {
            uiState.isLoading && uiState.dailyForecast.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.dailyForecast.isEmpty() -> {
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
                Column {
                    // Show error banner over stale data
                    if (uiState.error != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel.retry(latitude, longitude) }) {
                                Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }

                    val forecasts = uiState.dailyForecast
                    val overallMin = forecasts.minOfOrNull { it.temperatureMin } ?: 0.0
                    val overallMax = forecasts.maxOfOrNull { it.temperatureMax } ?: 100.0
                    var expandedIndex by rememberSaveable { mutableIntStateOf(-1) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    ) {
                        itemsIndexed(forecasts) { index, day ->
                        val hourlyData = uiState.hourlyByDate[day.date] ?: emptyList()
                        val moonEmoji = uiState.moonPhaseEvents[day.date]
                        DailyForecastCard(
                            day = day,
                            unit = uiState.temperatureUnit,
                            overallMin = overallMin,
                            overallMax = overallMax,
                            isExpanded = expandedIndex == index,
                            hourlyData = hourlyData,
                            clockFormat = uiState.clockFormat,
                            moonEmoji = moonEmoji,
                            onClick = {
                                expandedIndex = if (expandedIndex == index) -1 else index
                            },
                        )
                    }
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
    isExpanded: Boolean,
    hourlyData: List<HourlyForecast>,
    clockFormat: ClockFormat,
    moonEmoji: String? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Top row: date, icon, condition, moon glyph, details
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

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = day.condition.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                if (moonEmoji != null) {
                    Text(
                        text = moonEmoji,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }

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

            // Expandable hourly forecast
            AnimatedVisibility(
                visible = isExpanded && hourlyData.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hourly Forecast",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    HourlyForecastRow(hourlyData, unit, clockFormat)
                }
            }
        }
    }
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
