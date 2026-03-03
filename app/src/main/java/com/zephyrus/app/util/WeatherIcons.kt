package com.zephyrus.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector
import com.zephyrus.app.domain.model.WeatherCondition

object WeatherIcons {

    fun forCondition(condition: WeatherCondition, isDay: Boolean = true): ImageVector {
        return when (condition) {
            WeatherCondition.CLEAR_SKY -> if (isDay) Icons.Filled.WbSunny else Icons.Filled.WbTwilight
            WeatherCondition.MAINLY_CLEAR -> if (isDay) Icons.Filled.WbSunny else Icons.Filled.WbTwilight
            WeatherCondition.PARTLY_CLOUDY -> Icons.Filled.FilterDrama
            WeatherCondition.OVERCAST -> Icons.Filled.Cloud
            WeatherCondition.FOG,
            WeatherCondition.DEPOSITING_RIME_FOG -> Icons.Filled.WbCloudy
            WeatherCondition.LIGHT_DRIZZLE,
            WeatherCondition.MODERATE_DRIZZLE,
            WeatherCondition.DENSE_DRIZZLE -> Icons.Filled.Grain
            WeatherCondition.LIGHT_FREEZING_DRIZZLE,
            WeatherCondition.DENSE_FREEZING_DRIZZLE -> Icons.Filled.AcUnit
            WeatherCondition.LIGHT_RAIN,
            WeatherCondition.MODERATE_RAIN,
            WeatherCondition.HEAVY_RAIN -> Icons.Filled.WaterDrop
            WeatherCondition.LIGHT_FREEZING_RAIN,
            WeatherCondition.HEAVY_FREEZING_RAIN -> Icons.Filled.AcUnit
            WeatherCondition.LIGHT_SNOW,
            WeatherCondition.MODERATE_SNOW,
            WeatherCondition.HEAVY_SNOW,
            WeatherCondition.SNOW_GRAINS -> Icons.Filled.AcUnit
            WeatherCondition.LIGHT_RAIN_SHOWERS,
            WeatherCondition.MODERATE_RAIN_SHOWERS,
            WeatherCondition.VIOLENT_RAIN_SHOWERS -> Icons.Filled.WaterDrop
            WeatherCondition.LIGHT_SNOW_SHOWERS,
            WeatherCondition.HEAVY_SNOW_SHOWERS -> Icons.Filled.AcUnit
            WeatherCondition.THUNDERSTORM,
            WeatherCondition.THUNDERSTORM_LIGHT_HAIL,
            WeatherCondition.THUNDERSTORM_HEAVY_HAIL -> Icons.Filled.Thunderstorm
            WeatherCondition.UNKNOWN -> Icons.Filled.Cloud
        }
    }
}
