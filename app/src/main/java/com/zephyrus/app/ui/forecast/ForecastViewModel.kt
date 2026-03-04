package com.zephyrus.app.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.app.data.local.UserPreferences
import com.zephyrus.app.data.repository.WeatherRepository
import com.zephyrus.app.util.ErrorMessages
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
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferences.temperatureUnit.collect { unit ->
                _uiState.update { it.copy(temperatureUnit = unit) }
            }
        }
        viewModelScope.launch {
            userPreferences.clockFormat.collect { format ->
                _uiState.update { it.copy(clockFormat = format) }
            }
        }
    }

    fun loadForecast(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val hasExistingData = _uiState.value.dailyForecast.isNotEmpty()
            _uiState.update { it.copy(isLoading = !hasExistingData, error = null) }
            val unit = userPreferences.temperatureUnit.first()
            weatherRepository.getDailyWithHourlyForecast(latitude, longitude, unit)
                .onSuccess { (daily, hourly) ->
                    val hourlyByDate = hourly.groupBy { it.time.substringBefore("T") }
                    _uiState.update {
                        it.copy(
                            dailyForecast = daily,
                            hourlyByDate = hourlyByDate,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load forecast")
                    _uiState.update {
                        it.copy(isLoading = false, error = ErrorMessages.forForecast(e))
                    }
                }
        }
    }

    fun retry(latitude: Double, longitude: Double) {
        loadForecast(latitude, longitude)
    }
}
