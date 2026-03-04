package com.zephyrus.app.ui.current

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.zephyrus.app.domain.model.TemperatureUnit
import com.zephyrus.app.ui.components.HourlyForecastRow
import com.zephyrus.app.ui.components.ZephyrusTopAppBar
import com.zephyrus.app.util.WeatherIcons
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentScreen(
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    locationName: String = "Zephyrus",
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onLocationResolved: (Double, Double, String) -> Unit = { _, _, _ -> },
    viewModel: CurrentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.onLocationPermissionGranted()
            // If no location set yet, resolve GPS for initial load
            if (latitude == 0.0 && longitude == 0.0) {
                viewModel.resolveDeviceLocation { location ->
                    val displayName = if (location.admin1.isNotEmpty()) "${location.name}, ${location.admin1}" else location.name
                    onLocationResolved(location.latitude, location.longitude, displayName)
                }
            }
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    // Load weather when coordinates change
    LaunchedEffect(latitude, longitude) {
        if (latitude != 0.0 || longitude != 0.0) {
            viewModel.loadWeatherAt(latitude, longitude)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ZephyrusTopAppBar(
            locationName = locationName,
            subtitle = "Current Conditions",
            onRefreshClick = { viewModel.refresh() },
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
            onAboutClick = onAboutClick,
        )

        when {
            uiState.isLoading && uiState.currentWeather == null -> LoadingContent()
            uiState.error != null && uiState.currentWeather == null -> ErrorContent(
                message = uiState.error!!,
                onRetry = { viewModel.retry() },
            )
            else -> {
                Column {
                    // Show error banner over stale data
                    if (uiState.error != null) {
                        ErrorBanner(
                            message = uiState.error!!,
                            onRetry = { viewModel.retry() },
                        )
                    }
                    if (uiState.currentWeather != null) {
                        WeatherContent(
                            weather = uiState.currentWeather!!,
                            hourly = uiState.hourlyForecast,
                            unit = uiState.temperatureUnit,
                            clockFormat = uiState.clockFormat,
                        )
                    }
                }
            }
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
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun WeatherContent(
    weather: CurrentWeather,
    hourly: List<HourlyForecast>,
    unit: TemperatureUnit,
    clockFormat: ClockFormat,
) {
    val now = LocalDateTime.now()
    val currentHour = now.hour
    val todayDate = now.toLocalDate().toString()
    val futureHourly = hourly.filter { forecast ->
        val forecastDate = forecast.time.substringBefore("T")
        val forecastHour = forecast.time.substringAfter("T").take(2).toIntOrNull() ?: -1
        (forecastDate == todayDate && forecastHour >= currentHour) || forecastDate > todayDate
    }
    val filteredHourly = futureHourly.take(maxOf(8, futureHourly.size))

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
            HourlyForecastRow(filteredHourly, unit, clockFormat)
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
