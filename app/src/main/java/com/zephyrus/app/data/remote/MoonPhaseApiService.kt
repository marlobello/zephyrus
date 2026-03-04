package com.zephyrus.app.data.remote

import com.zephyrus.app.data.model.MoonPhaseResponse
import retrofit2.http.GET

interface MoonPhaseApiService {

    @GET("v1/current")
    suspend fun getCurrentMoonPhase(): MoonPhaseResponse
}
