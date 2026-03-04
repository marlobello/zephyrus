package com.zephyrus.app.data.remote

import com.zephyrus.app.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    @GET("v1/forecast")
    suspend fun getCurrentAndForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_PARAMS,
        @Query("hourly") hourly: String = HOURLY_PARAMS,
        @Query("daily") daily: String = DAILY_PARAMS,
        @Query("temperature_unit") temperatureUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windSpeedUnit: String = "mph",
        @Query("precipitation_unit") precipitationUnit: String = "inch",
        @Query("forecast_days") forecastDays: Int = 10,
        @Query("timezone") timezone: String = "auto",
    ): WeatherResponse

    companion object {
        const val CURRENT_PARAMS =
            "temperature_2m,apparent_temperature,relative_humidity_2m," +
            "wind_speed_10m,wind_direction_10m,surface_pressure," +
            "weather_code,is_day,uv_index,precipitation"

        const val HOURLY_PARAMS =
            "temperature_2m,relative_humidity_2m,weather_code,is_day"

        const val DAILY_PARAMS =
            "temperature_2m_max,temperature_2m_min,weather_code," +
            "wind_speed_10m_max,wind_direction_10m_dominant," +
            "precipitation_probability_max,uv_index_max," +
            "sunrise,sunset"
    }
}
