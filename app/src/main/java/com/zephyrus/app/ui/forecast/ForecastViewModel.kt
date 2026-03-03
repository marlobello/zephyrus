package com.zephyrus.app.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.app.data.local.UserPreferences
import com.zephyrus.app.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForecastUiState())
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    init {
        Timber.d("ForecastViewModel initialized")
        observeTemperatureUnit()
    }

    private fun observeTemperatureUnit() {
        viewModelScope.launch {
            userPreferences.temperatureUnit.collect { unit ->
                _uiState.update { it.copy(temperatureUnit = unit) }
            }
        }
    }

    fun loadForecast(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val unit = userPreferences.temperatureUnit.first()
            weatherRepository.getDailyForecast(latitude, longitude, unit)
                .onSuccess { forecast ->
                    _uiState.update { it.copy(dailyForecast = forecast, isLoading = false) }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load forecast")
                    _uiState.update {
                        it.copy(isLoading = false, error = "Unable to load forecast data.")
                    }
                }
        }
    }

    fun retry(latitude: Double, longitude: Double) {
        loadForecast(latitude, longitude)
    }
}
