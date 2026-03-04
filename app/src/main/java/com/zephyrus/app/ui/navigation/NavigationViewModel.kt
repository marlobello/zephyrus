package com.zephyrus.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.app.data.repository.LocationRepository
import com.zephyrus.app.domain.model.Location
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Shared ViewModel for navigation-level operations like GPS resolution.
 * Scoped to the activity so it's available across all screens.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
) : ViewModel() {

    fun resolveDeviceLocation(onResolved: (Location) -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            locationRepository.getDeviceLocation()
                .onSuccess { location ->
                    Timber.d("GPS resolved: %s (%.4f, %.4f)", location.name, location.latitude, location.longitude)
                    onResolved(location)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to resolve device location")
                    onError(e)
                }
        }
    }
}
