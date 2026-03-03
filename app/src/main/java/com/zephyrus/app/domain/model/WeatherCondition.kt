package com.zephyrus.app.domain.model

/**
 * Standard weather condition codes mapped from WMO weather codes.
 * Used across the app for condition display and icon selection.
 */
enum class WeatherCondition(val description: String) {
    CLEAR_SKY("Clear sky"),
    MAINLY_CLEAR("Mainly clear"),
    PARTLY_CLOUDY("Partly cloudy"),
    OVERCAST("Overcast"),
    FOG("Fog"),
    DEPOSITING_RIME_FOG("Freezing fog"),
    LIGHT_DRIZZLE("Light drizzle"),
    MODERATE_DRIZZLE("Drizzle"),
    DENSE_DRIZZLE("Heavy drizzle"),
    LIGHT_FREEZING_DRIZZLE("Light freezing drizzle"),
    DENSE_FREEZING_DRIZZLE("Freezing drizzle"),
    LIGHT_RAIN("Light rain"),
    MODERATE_RAIN("Rain"),
    HEAVY_RAIN("Heavy rain"),
    LIGHT_FREEZING_RAIN("Light freezing rain"),
    HEAVY_FREEZING_RAIN("Freezing rain"),
    LIGHT_SNOW("Light snow"),
    MODERATE_SNOW("Snow"),
    HEAVY_SNOW("Heavy snow"),
    SNOW_GRAINS("Snow grains"),
    LIGHT_RAIN_SHOWERS("Light rain showers"),
    MODERATE_RAIN_SHOWERS("Rain showers"),
    VIOLENT_RAIN_SHOWERS("Heavy rain showers"),
    LIGHT_SNOW_SHOWERS("Light snow showers"),
    HEAVY_SNOW_SHOWERS("Snow showers"),
    THUNDERSTORM("Thunderstorm"),
    THUNDERSTORM_LIGHT_HAIL("Thunderstorm with hail"),
    THUNDERSTORM_HEAVY_HAIL("Thunderstorm with heavy hail"),
    UNKNOWN("Unknown");

    companion object {
        /** Maps WMO weather interpretation codes to WeatherCondition. */
        fun fromWmoCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR_SKY
            1 -> MAINLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45 -> FOG
            48 -> DEPOSITING_RIME_FOG
            51 -> LIGHT_DRIZZLE
            53 -> MODERATE_DRIZZLE
            55 -> DENSE_DRIZZLE
            56 -> LIGHT_FREEZING_DRIZZLE
            57 -> DENSE_FREEZING_DRIZZLE
            61 -> LIGHT_RAIN
            63 -> MODERATE_RAIN
            65 -> HEAVY_RAIN
            66 -> LIGHT_FREEZING_RAIN
            67 -> HEAVY_FREEZING_RAIN
            71 -> LIGHT_SNOW
            73 -> MODERATE_SNOW
            75 -> HEAVY_SNOW
            77 -> SNOW_GRAINS
            80 -> LIGHT_RAIN_SHOWERS
            81 -> MODERATE_RAIN_SHOWERS
            82 -> VIOLENT_RAIN_SHOWERS
            85 -> LIGHT_SNOW_SHOWERS
            86 -> HEAVY_SNOW_SHOWERS
            95 -> THUNDERSTORM
            96 -> THUNDERSTORM_LIGHT_HAIL
            99 -> THUNDERSTORM_HEAVY_HAIL
            else -> UNKNOWN
        }
    }
}
