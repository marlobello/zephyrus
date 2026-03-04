package com.zephyrus.app.domain.model

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val pressure: Double,
    val uvIndex: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val pollen: PollenData?,
    val sunrise: String = "",
    val sunset: String = "",
    val timezone: String = "UTC",
    val moonPhase: Double = 0.0,
)

data class PollenData(
    val grassPollen: Double,
    val treePollen: Double,
    val weedPollen: Double,
) {
    /** Overall pollen level: Low (0-20), Moderate (20-50), High (50-100), Very High (100+). */
    fun overallLevel(): String {
        val max = maxOf(grassPollen, treePollen, weedPollen)
        return when {
            max < 20 -> "Low"
            max < 50 -> "Moderate"
            max < 100 -> "High"
            else -> "Very High"
        }
    }
}
