package com.zephyrus.app.di

import com.zephyrus.app.data.remote.AirQualityApiService
import com.zephyrus.app.data.remote.GeocodingApiService
import com.zephyrus.app.data.remote.MoonPhaseApiService
import com.zephyrus.app.data.remote.RainViewerApiService
import com.zephyrus.app.data.remote.WeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("weather")
    fun provideWeatherRetrofit(json: Json, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @Named("airQuality")
    fun provideAirQualityRetrofit(json: Json, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @Named("geocoding")
    fun provideGeocodingRetrofit(json: Json, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @Named("rainviewer")
    fun provideRainViewerRetrofit(json: Json, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.rainviewer.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @Named("moonphase")
    fun provideMoonPhaseRetrofit(json: Json, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://aa.usno.navy.mil/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(@Named("weather") retrofit: Retrofit): WeatherApiService {
        return retrofit.create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAirQualityApiService(@Named("airQuality") retrofit: Retrofit): AirQualityApiService {
        return retrofit.create(AirQualityApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeocodingApiService(@Named("geocoding") retrofit: Retrofit): GeocodingApiService {
        return retrofit.create(GeocodingApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRainViewerApiService(@Named("rainviewer") retrofit: Retrofit): RainViewerApiService {
        return retrofit.create(RainViewerApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMoonPhaseApiService(@Named("moonphase") retrofit: Retrofit): MoonPhaseApiService {
        return retrofit.create(MoonPhaseApiService::class.java)
    }
}
