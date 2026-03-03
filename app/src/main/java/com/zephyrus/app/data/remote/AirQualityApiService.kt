package com.zephyrus.app.data.remote

import com.zephyrus.app.data.model.AirQualityResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AirQualityApiService {

    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_PARAMS,
        @Query("timezone") timezone: String = "auto",
    ): AirQualityResponse

    companion object {
        const val CURRENT_PARAMS = "grass_pollen,birch_pollen,ragweed_pollen"
    }
}
