package com.zephyrus.app.data.remote

import com.zephyrus.app.data.model.MoonPhaseResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MoonPhaseApiService {

    @GET("api/moon/phases/year")
    suspend fun getPhasesByYear(
        @Query("year") year: Int,
    ): MoonPhaseResponse
}
