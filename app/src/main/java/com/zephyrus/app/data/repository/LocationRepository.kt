package com.zephyrus.app.data.repository

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.zephyrus.app.data.local.SavedLocationDao
import com.zephyrus.app.data.local.SavedLocationEntity
import com.zephyrus.app.data.model.GeocodingResult
import com.zephyrus.app.data.remote.GeocodingApiService
import com.zephyrus.app.domain.model.Location
import com.zephyrus.app.util.AirportCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geocodingApi: GeocodingApiService,
    private val savedLocationDao: SavedLocationDao,
) {
    // Cached device location — populated on first GPS request, reused for instant switching
    @Volatile
    private var cachedDeviceLocation: Location? = null

    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(): Result<Location> {
        // Return cache if available
        cachedDeviceLocation?.let { cached ->
            Timber.d("Returning cached device location: (%.4f, %.4f)", cached.latitude, cached.longitude)
            return Result.success(cached)
        }
        return refreshDeviceLocation()
    }

    @SuppressLint("MissingPermission")
    suspend fun refreshDeviceLocation(): Result<Location> = runCatching {
        Timber.d("Requesting fresh device location from GPS")
        val cancellationToken = CancellationTokenSource()
        try {
            val androidLocation = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token,
            ).await()

            if (androidLocation != null) {
                Timber.d("Device location: (%.4f, %.4f)", androidLocation.latitude, androidLocation.longitude)
                Location(
                    name = "Current Location",
                    latitude = androidLocation.latitude,
                    longitude = androidLocation.longitude,
                    isDeviceLocation = true,
                ).also { cachedDeviceLocation = it }
            } else {
                throw IllegalStateException("Unable to get device location")
            }
        } finally {
            cancellationToken.cancel()
        }
    }.onFailure { Timber.e(it, "Failed to get device location") }

    suspend fun searchLocations(query: String): Result<List<Location>> = runCatching {
        Timber.d("Searching locations for: %s", query)

        // Check if query is an airport code
        val searchQuery = AirportCodes.toCityName(query.uppercase()) ?: query

        val response = geocodingApi.searchLocations(searchQuery)
        response.results.map { it.toDomainLocation() }
    }.onFailure { Timber.e(it, "Failed to search locations") }

    val savedLocations: Flow<List<Location>> = savedLocationDao.getAll().map { entities ->
        entities.map { it.toDomainLocation() }
    }

    suspend fun saveLocation(location: Location) {
        Timber.d("Saving location: %s", location.name)
        savedLocationDao.insert(location.toEntity())
    }

    suspend fun deleteLocation(locationId: Long) {
        Timber.d("Deleting location: %d", locationId)
        savedLocationDao.deleteById(locationId)
    }
}

private fun GeocodingResult.toDomainLocation(): Location {
    return Location(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        country = country,
        admin1 = admin1,
    )
}

private fun SavedLocationEntity.toDomainLocation(): Location {
    return Location(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        country = country,
        admin1 = admin1,
    )
}

private fun Location.toEntity(): SavedLocationEntity {
    return SavedLocationEntity(
        name = name,
        latitude = latitude,
        longitude = longitude,
        country = country,
        admin1 = admin1,
        displayName = if (admin1.isNotEmpty()) "$name, $admin1" else name,
    )
}
