package com.zephyrus.app.ui.forecast

import com.zephyrus.app.domain.model.DailyForecast
import com.zephyrus.app.domain.model.TemperatureUnit

data class ForecastUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val dailyForecast: List<DailyForecast> = emptyList(),
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
)
