package com.zephyrus.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AirQualityResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current") val current: CurrentAirQuality? = null,
    @SerialName("hourly") val hourly: HourlyAirQuality? = null,
)

@Serializable
data class CurrentAirQuality(
    @SerialName("grass_pollen") val grassPollen: Double = 0.0,
    @SerialName("birch_pollen") val birchPollen: Double = 0.0,
    @SerialName("ragweed_pollen") val ragweedPollen: Double = 0.0,
)

@Serializable
data class HourlyAirQuality(
    val time: List<String> = emptyList(),
    @SerialName("grass_pollen") val grassPollen: List<Double> = emptyList(),
    @SerialName("birch_pollen") val birchPollen: List<Double> = emptyList(),
    @SerialName("ragweed_pollen") val ragweedPollen: List<Double> = emptyList(),
)
