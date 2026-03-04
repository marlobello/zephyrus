package com.zephyrus.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current") val current: CurrentData? = null,
    @SerialName("hourly") val hourly: HourlyData? = null,
    @SerialName("daily") val daily: DailyData? = null,
)

@Serializable
data class CurrentData(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relative_humidity_2m") val relativeHumidity: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDirection: Int,
    @SerialName("surface_pressure") val surfacePressure: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("is_day") val isDay: Int,
    @SerialName("uv_index") val uvIndex: Double = 0.0,
    @SerialName("precipitation") val precipitation: Double = 0.0,
)

@Serializable
data class HourlyData(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("relative_humidity_2m") val relativeHumidity: List<Int>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("is_day") val isDay: List<Int>,
)

@Serializable
data class DailyData(
    val time: List<String>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double>,
    @SerialName("wind_direction_10m_dominant") val windDirection: List<Int>,
    @SerialName("relative_humidity_2m_mean") val humidityMean: List<Int> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>,
    @SerialName("uv_index_max") val uvIndexMax: List<Double>,
)
