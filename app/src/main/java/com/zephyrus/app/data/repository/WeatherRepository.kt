package com.zephyrus.app.data.repository

import com.zephyrus.app.data.remote.AirQualityApiService
import com.zephyrus.app.data.remote.WeatherApiService
import com.zephyrus.app.data.remote.toCurrentWeather
import com.zephyrus.app.data.remote.toDailyForecasts
import com.zephyrus.app.data.remote.toHourlyForecasts
import com.zephyrus.app.domain.model.CurrentWeather
import com.zephyrus.app.domain.model.DailyForecast
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.TemperatureUnit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApiService,
    private val airQualityApi: AirQualityApiService,
) {
    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        unit: TemperatureUnit,
    ): Result<CurrentWeather> = runCatching {
        Timber.d("Fetching current weather for (%.4f, %.4f) in %s", latitude, longitude, unit)
        val tempUnit = if (unit == TemperatureUnit.CELSIUS) "celsius" else "fahrenheit"

        val weatherResponse = weatherApi.getCurrentAndForecast(
            latitude = latitude,
            longitude = longitude,
            temperatureUnit = tempUnit,
            forecastDays = 1,
        )

        val airQualityResponse = try {
            airQualityApi.getAirQuality(latitude, longitude)
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch air quality data, continuing without pollen")
            null
        }

        weatherResponse.toCurrentWeather(airQualityResponse)
            ?: throw IllegalStateException("No current weather data in response")
    }.onFailure { Timber.e(it, "Failed to fetch current weather") }

    suspend fun getHourlyForecast(
        latitude: Double,
        longitude: Double,
        unit: TemperatureUnit,
    ): Result<List<HourlyForecast>> = runCatching {
        Timber.d("Fetching hourly forecast for (%.4f, %.4f)", latitude, longitude)
        val tempUnit = if (unit == TemperatureUnit.CELSIUS) "celsius" else "fahrenheit"

        val response = weatherApi.getCurrentAndForecast(
            latitude = latitude,
            longitude = longitude,
            temperatureUnit = tempUnit,
            forecastDays = 1,
        )

        response.hourly?.toHourlyForecasts() ?: emptyList()
    }.onFailure { Timber.e(it, "Failed to fetch hourly forecast") }

    suspend fun getDailyForecast(
        latitude: Double,
        longitude: Double,
        unit: TemperatureUnit,
    ): Result<List<DailyForecast>> = runCatching {
        Timber.d("Fetching 10-day forecast for (%.4f, %.4f)", latitude, longitude)
        val tempUnit = if (unit == TemperatureUnit.CELSIUS) "celsius" else "fahrenheit"

        val response = weatherApi.getCurrentAndForecast(
            latitude = latitude,
            longitude = longitude,
            temperatureUnit = tempUnit,
            forecastDays = 10,
        )

        response.daily?.toDailyForecasts() ?: emptyList()
    }.onFailure { Timber.e(it, "Failed to fetch daily forecast") }
}
