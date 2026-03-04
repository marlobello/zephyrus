package com.zephyrus.app.data.repository

import com.zephyrus.app.data.remote.AirQualityApiService
import com.zephyrus.app.data.remote.MoonPhaseApiService
import com.zephyrus.app.data.remote.WeatherApiService
import com.zephyrus.app.data.remote.toCurrentWeather
import com.zephyrus.app.data.remote.toDailyForecasts
import com.zephyrus.app.data.remote.toHourlyForecasts
import com.zephyrus.app.data.remote.withRetry
import com.zephyrus.app.domain.model.CurrentWeather
import com.zephyrus.app.domain.model.DailyForecast
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.MoonPhaseData
import com.zephyrus.app.domain.model.TemperatureUnit
import com.zephyrus.app.util.MoonPhaseCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApiService,
    private val airQualityApi: AirQualityApiService,
    private val moonPhaseApi: MoonPhaseApiService,
) {
    // In-memory spatial cache for grid weather data, keyed by rounded lat/lon
    private val gridCache = ConcurrentHashMap<Long, GridPointData>()

    /** Rounds lat/lon to 0.05° precision and packs into a single Long key. */
    private fun cacheKey(lat: Double, lon: Double): Long {
        val rlat = (lat * 20).roundToLong()  // 0.05° buckets
        val rlon = (lon * 20).roundToLong()
        return rlat * 1_000_000L + rlon
    }

    fun clearGridCache() {
        val size = gridCache.size
        gridCache.clear()
        Timber.d("Grid cache cleared (%d entries)", size)
    }

    suspend fun getMoonPhase(): MoonPhaseData {
        return try {
            val response = withRetry(tag = "MoonPhase") {
                moonPhaseApi.getCurrentMoonPhase()
            }
            MoonPhaseData(
                phaseName = response.phase,
                illumination = response.illumination,
                emoji = response.emoji,
            )
        } catch (e: Exception) {
            Timber.w(e, "Moon phase API failed, using local calculation")
            MoonPhaseCalculator.toMoonPhaseData()
        }
    }
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
        radiusDegrees: Double = 4.3,
    ): Result<Array<Array<GridPointData>>> = runCatching {
        val tempUnit = if (unit == TemperatureUnit.CELSIUS) "celsius" else "fahrenheit"
        val latStep = (2 * radiusDegrees) / (gridSize - 1)
        val lonStep = (2 * radiusDegrees) / (gridSize - 1)
        val startLat = centerLat + radiusDegrees
        val startLon = centerLon - radiusDegrees

        // Build list of all grid points, checking cache first
        data class GridRequest(val row: Int, val col: Int, val lat: Double, val lon: Double, val key: Long)

        val cached = mutableListOf<Triple<Int, Int, GridPointData>>()
        val uncached = mutableListOf<GridRequest>()

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val lat = startLat - row * latStep
                val lon = startLon + col * lonStep
                val key = cacheKey(lat, lon)
                val hit = gridCache[key]
                if (hit != null) {
                    cached.add(Triple(row, col, hit))
                } else {
                    uncached.add(GridRequest(row, col, lat, lon, key))
                }
            }
        }

        Timber.d("Grid %dx%d: %d cached, %d to fetch", gridSize, gridSize, cached.size, uncached.size)

        val semaphore = kotlinx.coroutines.sync.Semaphore(8)

        val fetched = if (uncached.isNotEmpty()) {
            coroutineScope {
                uncached.map { req ->
                    async {
                        semaphore.withPermit {
                            Triple(req.row, req.col, try {
                                withRetry(maxRetries = 2, tag = "Grid") {
                                    val resp = weatherApi.getCurrentAndForecast(
                                        latitude = req.lat,
                                        longitude = req.lon,
                                        temperatureUnit = tempUnit,
                                        forecastDays = 1,
                                    )
                                    val data = GridPointData(
                                        temperature = resp.current?.temperature ?: 0.0,
                                        humidity = resp.current?.relativeHumidity?.toDouble() ?: 0.0,
                                        pressure = resp.current?.surfacePressure ?: 0.0,
                                        precipitation = resp.current?.precipitation ?: 0.0,
                                    )
                                    gridCache[req.key] = data
                                    data
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Grid fetch failed at (%.4f, %.4f)", req.lat, req.lon)
                                GridPointData(0.0, 0.0, 0.0, 0.0)
                            })
                        }
                    }
                }.awaitAll()
            }
        } else {
            emptyList()
        }

        val grid = Array(gridSize) { Array(gridSize) { GridPointData(0.0, 0.0, 0.0, 0.0) } }
        cached.forEach { (row, col, data) -> grid[row][col] = data }
        fetched.forEach { (row, col, data) -> grid[row][col] = data }
        Timber.d("Grid complete: %d from cache, %d freshly fetched", cached.size, fetched.size)
        grid
    }.onFailure { Timber.e(it, "Failed to fetch grid weather data") }
}
