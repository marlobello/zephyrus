package com.zephyrus.app.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.app.data.local.UserPreferences
import com.zephyrus.app.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState())
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private var lastFetchLat = 0.0
    private var lastFetchLon = 0.0
    private var lastFetchZoom = 0.0

    init {
        viewModelScope.launch {
            userPreferences.temperatureUnit.collect { unit ->
                _uiState.update { it.copy(temperatureUnit = unit) }
            }
        }
    }

    /**
     * Called when the map viewport changes (after debounce).
     * Uses the actual visible bounding box span to determine grid coverage.
     * Skips fetch if the viewport hasn't changed significantly.
     */
    fun onViewportChanged(
        centerLat: Double,
        centerLon: Double,
        zoomLevel: Double,
        visibleLatSpan: Double = 0.0,
        visibleLonSpan: Double = 0.0,
    ) {
        // Use actual visible span with 20% padding, fall back to formula if span unavailable
        val radius = if (visibleLatSpan > 0 && visibleLonSpan > 0) {
            (maxOf(visibleLatSpan, visibleLonSpan) / 2.0 * 1.2).coerceIn(0.1, 10.0)
        } else {
            (180.0 / 2.0.pow(zoomLevel - 1)).coerceIn(0.1, 5.0)
        }

        // Skip if viewport barely changed
        if (lastFetchZoom != 0.0) {
            val movedLat = abs(centerLat - lastFetchLat)
            val movedLon = abs(centerLon - lastFetchLon)
            val threshold = radius * 0.1
            if (movedLat < threshold && movedLon < threshold && abs(zoomLevel - lastFetchZoom) < 0.5) {
                Timber.d("Viewport change too small, skipping fetch")
                return
            }
        }

        lastFetchLat = centerLat
        lastFetchLon = centerLon
        lastFetchZoom = zoomLevel

        // Cancel any in-flight fetch
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    centerLatitude = centerLat,
                    centerLongitude = centerLon,
                    radiusDegrees = radius,
                )
            }
            val unit = userPreferences.temperatureUnit.first()
            Timber.d("Fetching grid: center=(%.4f, %.4f), zoom=%.1f, radius=%.2f°",
                centerLat, centerLon, zoomLevel, radius)
            weatherRepository.getGridWeatherData(centerLat, centerLon, unit, radiusDegrees = radius)
                .onSuccess { grid ->
                    val rows = grid.size
                    val cols = grid[0].size
                    val temps = Array(rows) { r -> DoubleArray(cols) { c -> grid[r][c].temperature } }
                    val humidity = Array(rows) { r -> DoubleArray(cols) { c -> grid[r][c].humidity } }
                    val pressure = Array(rows) { r -> DoubleArray(cols) { c -> grid[r][c].pressure } }
                    val precipitation = Array(rows) { r -> DoubleArray(cols) { c -> grid[r][c].precipitation } }
                    Timber.d("Grid data loaded: %dx%d", rows, cols)
                    _uiState.update {
                        it.copy(
                            gridTemperatures = temps,
                            gridHumidity = humidity,
                            gridPressure = pressure,
                            gridPrecipitation = precipitation,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load grid data")
                    _uiState.update {
                        it.copy(isLoading = false, error = "Unable to load map data.")
                    }
                }
        }
    }

    fun setActiveLayer(layer: MapLayer) {
        _uiState.update { it.copy(activeLayer = layer) }
    }

    fun refresh() {
        weatherRepository.clearGridCache()
        val prevZoom = lastFetchZoom
        lastFetchZoom = 0.0 // Force re-fetch
        val state = _uiState.value
        onViewportChanged(state.centerLatitude, state.centerLongitude, prevZoom.coerceAtLeast(8.0))
    }
}
