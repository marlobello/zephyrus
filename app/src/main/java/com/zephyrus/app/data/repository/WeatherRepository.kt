package com.zephyrus.app.data.repository

import com.zephyrus.app.data.remote.AirQualityApiService
import com.zephyrus.app.data.remote.WeatherApiService
import com.zephyrus.app.data.remote.toCurrentWeather
import com.zephyrus.app.data.remote.toDailyForecasts
import com.zephyrus.app.data.remote.toHourlyForecasts
import com.zephyrus.app.data.remote.withRetry
import com.zephyrus.app.domain.model.CurrentWeather
import com.zephyrus.app.domain.model.DailyForecast
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.TemperatureUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withPermit
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

        val weatherResponse = withRetry(tag = "CurrentWeather") {
            weatherApi.getCurrentAndForecast(
                latitude = latitude,
                longitude = longitude,
                temperatureUnit = tempUnit,
                forecastDays = 1,
            )
        }

        val airQualityResponse = try {
            withRetry(tag = "AirQuality") {
                airQualityApi.getAirQuality(latitude, longitude)
            }
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

        val response = withRetry(tag = "HourlyForecast") {
            weatherApi.getCurrentAndForecast(
                latitude = latitude,
                longitude = longitude,
                temperatureUnit = tempUnit,
                forecastDays = 2,
            )
        }

        response.hourly?.toHourlyForecasts() ?: emptyList()
    }.onFailure { Timber.e(it, "Failed to fetch hourly forecast") }

    suspend fun getDailyForecast(
        latitude: Double,
        longitude: Double,
        unit: TemperatureUnit,
    ): Result<List<DailyForecast>> = runCatching {
        Timber.d("Fetching 10-day forecast for (%.4f, %.4f)", latitude, longitude)
        val tempUnit = if (unit == TemperatureUnit.CELSIUS) "celsius" else "fahrenheit"

        val response = withRetry(tag = "DailyForecast") {
            weatherApi.getCurrentAndForecast(
                latitude = latitude,
                longitude = longitude,
                temperatureUnit = tempUnit,
                forecastDays = 10,
            )
        }

        response.daily?.toDailyForecasts() ?: emptyList()
    }.onFailure { Timber.e(it, "Failed to fetch daily forecast") }

    suspend fun getDailyWithHourlyForecast(
        latitude: Double,
        longitude: Double,
        unit: TemperatureUnit,
    ): Result<Pair<List<DailyForecast>, List<HourlyForecast>>> = runCatching {
        Timber.d("Fetching 10-day forecast with hourly for (%.4f, %.4f)", latitude, longitude)
        val tempUnit = if (unit == TemperatureUnit.CELSIUS) "celsius" else "fahrenheit"

        val response = withRetry(tag = "DailyWithHourly") {
            weatherApi.getCurrentAndForecast(
                latitude = latitude,
                longitude = longitude,
                temperatureUnit = tempUnit,
                forecastDays = 10,
            )
        }

        val daily = response.daily?.toDailyForecasts() ?: emptyList()
        val hourly = response.hourly?.toHourlyForecasts() ?: emptyList()
        Pair(daily, hourly)
    }.onFailure { Timber.e(it, "Failed to fetch forecast with hourly") }

    /**
     * Grid weather data for a single point: temperature and humidity.
     */
    data class GridPointData(
        val temperature: Double,
        val humidity: Double,
        val pressure: Double,
        val precipitation: Double,
    )

    /**
     * Fetches current temperature and humidity for a grid of points covering a 300-mile radius.
     * Uses an 8×8 grid (~75 miles between sample points) with concurrency limiting.
     * Returns a 2D array [rows][cols] of GridPointData.
     */
    suspend fun getGridWeatherData(
        centerLat: Double,
        centerLon: Double,
        unit: TemperatureUnit,
        gridSize: Int = 8,
        radiusDegrees: Double = 4.3, // ~300 miles radius
    ): Result<Array<Array<GridPointData>>> = runCatching {
        Timber.d("Fetching %dx%d grid data around (%.4f, %.4f), radius=%.1f°",
            gridSize, gridSize, centerLat, centerLon, radiusDegrees)
        val tempUnit = if (unit == TemperatureUnit.CELSIUS) "celsius" else "fahrenheit"
        val latStep = (2 * radiusDegrees) / (gridSize - 1)
        val lonStep = (2 * radiusDegrees) / (gridSize - 1)
        val startLat = centerLat + radiusDegrees
        val startLon = centerLon - radiusDegrees

        val semaphore = kotlinx.coroutines.sync.Semaphore(8)

        coroutineScope {
            val deferreds = (0 until gridSize).flatMap { row ->
                (0 until gridSize).map { col ->
                    val lat = startLat - row * latStep
                    val lon = startLon + col * lonStep
                    async {
                        semaphore.withPermit {
                            Triple(row, col, try {
                                withRetry(maxRetries = 2, tag = "Grid") {
                                    val resp = weatherApi.getCurrentAndForecast(
                                        latitude = lat,
                                        longitude = lon,
                                        temperatureUnit = tempUnit,
                                        forecastDays = 1,
                                    )
                                    GridPointData(
                                        temperature = resp.current?.temperature ?: 0.0,
                                        humidity = resp.current?.relativeHumidity?.toDouble() ?: 0.0,
                                        pressure = resp.current?.surfacePressure ?: 0.0,
                                        precipitation = resp.current?.precipitation ?: 0.0,
                                    )
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Grid fetch failed at (%.4f, %.4f)", lat, lon)
                                GridPointData(0.0, 0.0, 0.0, 0.0)
                            })
                        }
                    }
                }
            }
            val results = deferreds.awaitAll()
            val grid = Array(gridSize) { Array(gridSize) { GridPointData(0.0, 0.0, 0.0, 0.0) } }
            results.forEach { (row, col, data) -> grid[row][col] = data }
            Timber.d("Grid data loaded: %d points fetched", results.size)
            grid
        }
    }.onFailure { Timber.e(it, "Failed to fetch grid weather data") }
}
