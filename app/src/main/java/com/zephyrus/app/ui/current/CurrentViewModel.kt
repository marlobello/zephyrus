package com.zephyrus.app.ui.current

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.app.data.local.UserPreferences
import com.zephyrus.app.data.repository.LocationRepository
import com.zephyrus.app.data.repository.WeatherRepository
import com.zephyrus.app.domain.model.Location
import com.zephyrus.app.domain.model.TemperatureUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val REFRESH_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

@HiltViewModel
class CurrentViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrentUiState())
    val uiState: StateFlow<CurrentUiState> = _uiState.asStateFlow()

    private var lastRefreshTimeMs: Long = 0L
    private var autoRefreshJob: Job? = null
    private var lastLoadedLat: Double = Double.NaN
    private var lastLoadedLon: Double = Double.NaN

    init {
        Timber.d("CurrentViewModel initialized")
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                userPreferences.temperatureUnit,
                userPreferences.clockFormat,
            ) { tempUnit, clockFmt -> tempUnit to clockFmt }
                .collect { (unit, clockFmt) ->
                    val unitChanged = _uiState.value.temperatureUnit != unit
                    _uiState.update { it.copy(temperatureUnit = unit, clockFormat = clockFmt) }
                    if (unitChanged && !lastLoadedLat.isNaN()) {
                        loadWeather(lastLoadedLat, lastLoadedLon)
                    }
                }
        }
    }

    fun onLocationPermissionGranted() {
        Timber.d("Location permission granted")
        _uiState.update { it.copy(hasLocationPermission = true) }
    }

    fun onLocationPermissionDenied() {
        Timber.w("Location permission denied")
        _uiState.update {
            it.copy(
                hasLocationPermission = false,
                isLoading = false,
                error = "Location permission is needed to show weather for your area. Search for a location instead.",
            )
        }
    }

    /**
     * Load weather for coordinates passed from the shared navigation state.
     * Skips reload if coordinates haven't changed since last load.
     */
    fun loadWeatherAt(latitude: Double, longitude: Double) {
        if (latitude == lastLoadedLat && longitude == lastLoadedLon) return
        Timber.d("Loading weather at (%.4f, %.4f)", latitude, longitude)
        lastLoadedLat = latitude
        lastLoadedLon = longitude
        loadWeather(latitude, longitude)
        startAutoRefresh(latitude, longitude)
    }

    /**
     * Resolve device GPS location. Returns the resolved Location via callback.
     */
    fun resolveDeviceLocation(onResolved: (Location) -> Unit) {
        Timber.d("Resolving device location")
        if (!_uiState.value.hasLocationPermission) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            locationRepository.getDeviceLocation()
                .onSuccess { location ->
                    Timber.d("Device location resolved: %s (%.4f, %.4f)", location.name, location.latitude, location.longitude)
                    onResolved(location)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to get device location")
                    _uiState.update {
                        it.copy(isLoading = false, error = "Unable to determine your location.")
                    }
                }
        }
    }

    fun refresh() {
        Timber.d("Manual refresh requested")
        if (!lastLoadedLat.isNaN() && !lastLoadedLon.isNaN()) {
            loadWeather(lastLoadedLat, lastLoadedLon)
        }
    }

    fun retry() {
        Timber.d("Retrying weather load")
        refresh()
    }

    private fun startAutoRefresh(latitude: Double, longitude: Double) {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                Timber.d("Auto-refreshing weather data")
                loadWeather(latitude, longitude)
            }
        }
    }

    private fun loadWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val unit = _uiState.value.temperatureUnit

            weatherRepository.getCurrentWeather(latitude, longitude, unit)
                .onSuccess { weather ->
                    _uiState.update { it.copy(currentWeather = weather) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "Unable to load weather data.") }
                }

            weatherRepository.getHourlyForecast(latitude, longitude, unit)
                .onSuccess { hourly ->
                    lastRefreshTimeMs = System.currentTimeMillis()
                    Timber.d("Weather data refreshed at %d", lastRefreshTimeMs)
                    _uiState.update { it.copy(hourlyForecast = hourly, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }
}
