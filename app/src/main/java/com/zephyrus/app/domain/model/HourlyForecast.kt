package com.zephyrus.app.domain.model

data class HourlyForecast(
    val time: String,
    val temperature: Double,
    val humidity: Int,
    val condition: WeatherCondition,
    val isDay: Boolean,
)
