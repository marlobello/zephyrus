package com.zephyrus.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface RainViewerApiService {
    @GET("public/weather-maps.json")
    suspend fun getWeatherMaps(): RainViewerResponse
}

@Serializable
data class RainViewerResponse(
    val version: String = "",
    val generated: Long = 0,
    val host: String = "",
    val radar: RainViewerRadar = RainViewerRadar(),
)

@Serializable
data class RainViewerRadar(
    val past: List<RainViewerFrame> = emptyList(),
    val nowcast: List<RainViewerFrame> = emptyList(),
)

@Serializable
data class RainViewerFrame(
    val time: Long = 0,
    val path: String = "",
)
