package com.zephyrus.app.ui.current

import com.zephyrus.app.domain.model.ClockFormat
import com.zephyrus.app.domain.model.CurrentWeather
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.TemperatureUnit

data class CurrentUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentWeather: CurrentWeather? = null,
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val clockFormat: ClockFormat = ClockFormat.TWELVE_HOUR,
    val hasLocationPermission: Boolean = false,
)
