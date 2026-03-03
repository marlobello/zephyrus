package com.zephyrus.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
)

@Serializable
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "",
    @SerialName("admin1") val admin1: String = "",
    @SerialName("country_code") val countryCode: String = "",
)
