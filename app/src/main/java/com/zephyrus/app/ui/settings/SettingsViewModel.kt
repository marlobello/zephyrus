package com.zephyrus.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.app.data.local.UserPreferences
import com.zephyrus.app.domain.model.ClockFormat
import com.zephyrus.app.domain.model.TemperatureUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val clockFormat: ClockFormat = ClockFormat.TWELVE_HOUR,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferences.temperatureUnit,
                userPreferences.clockFormat,
            ) { tempUnit, clockFmt ->
                SettingsUiState(temperatureUnit = tempUnit, clockFormat = clockFmt)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            Timber.d("Setting temperature unit to %s", unit)
            userPreferences.setTemperatureUnit(unit)
        }
    }

    fun setClockFormat(format: ClockFormat) {
        viewModelScope.launch {
            Timber.d("Setting clock format to %s", format)
            userPreferences.setClockFormat(format)
        }
    }
}
