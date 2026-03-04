package com.zephyrus.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zephyrus.app.domain.model.ClockFormat
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.TemperatureUnit
import com.zephyrus.app.util.WeatherIcons
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val output12h = DateTimeFormatter.ofPattern("h a")
private val output24h = DateTimeFormatter.ofPattern("HH:mm")

fun formatHourlyTime(isoTime: String, clockFormat: ClockFormat): String {
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

@Composable
fun HourlyCard(hour: HourlyForecast, unit: TemperatureUnit, clockFormat: ClockFormat) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = "Humidity",
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${hour.humidity}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun HourlyForecastRow(
    hourlyData: List<HourlyForecast>,
    unit: TemperatureUnit,
    clockFormat: ClockFormat,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(hourlyData) { hour ->
            HourlyCard(hour, unit, clockFormat)
        }
    }
}
