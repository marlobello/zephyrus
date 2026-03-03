package com.zephyrus.app.ui.current

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyrus.app.domain.model.CardinalDirection
import com.zephyrus.app.domain.model.ClockFormat
import com.zephyrus.app.domain.model.CurrentWeather
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.Location
import com.zephyrus.app.domain.model.TemperatureUnit
import com.zephyrus.app.util.WeatherIcons
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentScreen(
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLocationResolved: (Double, Double) -> Unit = { _, _ -> },
    pendingSearchLocation: Location? = null,
    onSearchLocationConsumed: () -> Unit = {},
    pendingUseDeviceLocation: Boolean = false,
    onDeviceLocationConsumed: () -> Unit = {},
    viewModel: CurrentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) viewModel.onLocationPermissionGranted()
        else viewModel.onLocationPermissionDenied()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    // Handle location selected from search
    LaunchedEffect(pendingSearchLocation) {
        pendingSearchLocation?.let { location ->
            viewModel.selectLocation(location)
            onSearchLocationConsumed()
        }
    }

    // Handle "Current Location" selected from search
    LaunchedEffect(pendingUseDeviceLocation) {
        if (pendingUseDeviceLocation) {
            viewModel.switchToDeviceLocation()
            onDeviceLocationConsumed()
        }
    }

    // Push active location up to parent for sharing with other tabs
    LaunchedEffect(uiState.location) {
        uiState.location?.let { loc ->
            onLocationResolved(loc.latitude, loc.longitude)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = uiState.location?.let { loc ->
                        if (loc.admin1.isNotEmpty()) "${loc.name}, ${loc.admin1}" else loc.name
                    } ?: "Zephyrus",
                )
            },
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
                Box {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Search Location") },
                            onClick = {
                                menuExpanded = false
                                onSearchClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menuExpanded = false
                                onSettingsClick()
                            },
                        )
                    }
                }
            },
        )

        when {
            uiState.isLoading -> LoadingContent()
            uiState.error != null -> ErrorContent(
                message = uiState.error!!,
                onRetry = { viewModel.retry() },
            )
            uiState.currentWeather != null -> WeatherContent(
                weather = uiState.currentWeather!!,
                hourly = uiState.hourlyForecast,
                unit = uiState.temperatureUnit,
                clockFormat = uiState.clockFormat,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun WeatherContent(
    weather: CurrentWeather,
    hourly: List<HourlyForecast>,
    unit: TemperatureUnit,
    clockFormat: ClockFormat,
) {
    val currentHour = LocalDateTime.now().hour
    val filteredHourly = hourly.filter { forecast ->
        val forecastHour = forecast.time.substringAfter("T").take(2).toIntOrNull() ?: -1
        forecastHour >= currentHour
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Main temperature and condition
        Icon(
            imageVector = WeatherIcons.forCondition(weather.condition, weather.isDay),
            contentDescription = weather.condition.description,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${weather.temperature.toInt()}${unit.label()}",
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = weather.condition.description,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Feels like ${weather.feelsLike.toInt()}${unit.label()}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Detail cards in a grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailCard("Wind", "${weather.windSpeed.toInt()} mph", Modifier.weight(1f))
            WindDirectionCard(
                degrees = weather.windDirection,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailCard("Humidity", "${weather.humidity}%", Modifier.weight(1f))
            DetailCard("Pressure", "${weather.pressure.toInt()} hPa", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailCard(
                "Pollen",
                weather.pollen?.overallLevel() ?: "N/A",
                Modifier.weight(1f),
            )
            DetailCard("UV Index", "%.1f".format(weather.uvIndex), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hourly forecast
        if (filteredHourly.isNotEmpty()) {
            Text(
                text = "Hourly Forecast",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(filteredHourly) { hour ->
                    HourlyCard(hour, unit, clockFormat)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun WindDirectionCard(degrees: Int, modifier: Modifier = Modifier) {
    val cardinal = CardinalDirection.fromDegrees(degrees)
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Wind Direction",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = "Wind from ${cardinal.label}",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(degrees.toFloat()),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = cardinal.label,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun HourlyCard(hour: HourlyForecast, unit: TemperatureUnit, clockFormat: ClockFormat) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val timeLabel = formatHourlyTime(hour.time, clockFormat)
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = WeatherIcons.forCondition(hour.condition, hour.isDay),
                contentDescription = hour.condition.description,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${hour.temperature.toInt()}${unit.label()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${hour.humidity}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val output12h = DateTimeFormatter.ofPattern("h a")
private val output24h = DateTimeFormatter.ofPattern("HH:mm")

private fun formatHourlyTime(isoTime: String, clockFormat: ClockFormat): String {
    return try {
        val dt = LocalDateTime.parse(isoTime, inputFormatter)
        when (clockFormat) {
            ClockFormat.TWELVE_HOUR -> dt.format(output12h)
            ClockFormat.TWENTY_FOUR_HOUR -> dt.format(output24h)
        }
    } catch (_: Exception) {
        isoTime.substringAfter("T").take(5)
    }
}
