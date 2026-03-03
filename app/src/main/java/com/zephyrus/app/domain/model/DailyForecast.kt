package com.zephyrus.app.domain.model

data class DailyForecast(
    val date: String,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val condition: WeatherCondition,
    val windSpeedMax: Double,
    val windDirection: Int,
    val humidity: Int,
    val precipitationProbability: Int,
    val uvIndexMax: Double,
    val pollen: PollenData?,
)
