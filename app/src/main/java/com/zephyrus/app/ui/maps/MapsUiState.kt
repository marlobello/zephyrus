package com.zephyrus.app.ui.maps

import com.zephyrus.app.domain.model.TemperatureUnit

data class MapsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val gridTemperatures: Array<DoubleArray>? = null,
    val gridHumidity: Array<DoubleArray>? = null,
    val gridPressure: Array<DoubleArray>? = null,
    val gridPrecipitation: Array<DoubleArray>? = null,
    val activeLayer: MapLayer = MapLayer.TEMPERATURE,
    val centerLatitude: Double = 0.0,
    val centerLongitude: Double = 0.0,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val radiusDegrees: Double = 4.3,
)
