package com.zephyrus.app.data.remote

import com.zephyrus.app.data.model.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {

    @GET("v1/search")
    suspend fun searchLocations(
        @Query("name") query: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse
}
