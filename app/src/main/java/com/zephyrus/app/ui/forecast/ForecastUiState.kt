package com.zephyrus.app.ui.forecast

import com.zephyrus.app.domain.model.ClockFormat
import com.zephyrus.app.domain.model.DailyForecast
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.TemperatureUnit

data class ForecastUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val dailyForecast: List<DailyForecast> = emptyList(),
    val hourlyByDate: Map<String, List<HourlyForecast>> = emptyMap(),
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val clockFormat: ClockFormat = ClockFormat.TWELVE_HOUR,
)
