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

    init {
        Timber.d("CurrentViewModel initialized")
        observePreferences()
        startAutoRefresh()
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
                    if (unitChanged) {
                        _uiState.value.location?.let { loadWeather(it) }
                    }
                }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                val location = _uiState.value.location
                if (location != null) {
                    Timber.d("Auto-refreshing weather data for %s", location.name)
                    loadWeather(location)
                }
            }
        }
    }

    fun onLocationPermissionGranted() {
        Timber.d("Location permission granted")
        _uiState.update { it.copy(hasLocationPermission = true) }
        // Only load device location if no location is set yet
        if (_uiState.value.location == null) {
            loadDeviceLocation()
        }
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

    fun selectLocation(location: Location) {
        Timber.d("Location selected: %s", location.name)
        _uiState.update { it.copy(location = location) }
        loadWeather(location)
    }

    fun switchToDeviceLocation() {
        Timber.d("Switching to device location")
        if (_uiState.value.hasLocationPermission) {
            loadDeviceLocation()
        }
    }

    fun refresh() {
        Timber.d("Manual refresh requested")
        val location = _uiState.value.location
        if (location != null) {
            loadWeather(location)
        } else if (_uiState.value.hasLocationPermission) {
            loadDeviceLocation()
        }
    }

    fun retry() {
        Timber.d("Retrying weather load")
        refresh()
    }

    private fun loadDeviceLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            locationRepository.getDeviceLocation()
                .onSuccess { location ->
                    _uiState.update { it.copy(location = location) }
                    loadWeather(location)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to get device location")
                    _uiState.update {
                        it.copy(isLoading = false, error = "Unable to determine your location.")
                    }
                }
        }
    }

    private fun loadWeather(location: Location) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val unit = _uiState.value.temperatureUnit

            weatherRepository.getCurrentWeather(location.latitude, location.longitude, unit)
                .onSuccess { weather ->
                    _uiState.update { it.copy(currentWeather = weather) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "Unable to load weather data.") }
                }

            weatherRepository.getHourlyForecast(location.latitude, location.longitude, unit)
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
