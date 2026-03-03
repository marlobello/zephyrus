package com.zephyrus.app.domain.model

enum class TemperatureUnit {
    FAHRENHEIT,
    CELSIUS;

    fun label(): String = when (this) {
        FAHRENHEIT -> "°F"
        CELSIUS -> "°C"
    }
}
