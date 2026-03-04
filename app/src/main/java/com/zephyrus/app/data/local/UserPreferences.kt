package com.zephyrus.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zephyrus.app.domain.model.ClockFormat
import com.zephyrus.app.domain.model.TemperatureUnit
import com.zephyrus.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zephyrus_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val temperatureUnitKey = stringPreferencesKey("temperature_unit")
    private val clockFormatKey = stringPreferencesKey("clock_format")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val temperatureUnit: Flow<TemperatureUnit> = context.dataStore.data.map { prefs ->
        val value = prefs[temperatureUnitKey] ?: TemperatureUnit.FAHRENHEIT.name
        try {
            TemperatureUnit.valueOf(value)
        } catch (_: IllegalArgumentException) {
            Timber.w("Invalid temperature unit preference: %s, falling back to FAHRENHEIT", value)
            TemperatureUnit.FAHRENHEIT
        }
    }

    val clockFormat: Flow<ClockFormat> = context.dataStore.data.map { prefs ->
        val value = prefs[clockFormatKey] ?: ClockFormat.TWELVE_HOUR.name
        try {
            ClockFormat.valueOf(value)
        } catch (_: IllegalArgumentException) {
            Timber.w("Invalid clock format preference: %s, falling back to TWELVE_HOUR", value)
            ClockFormat.TWELVE_HOUR
        }
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        Timber.d("Setting temperature unit to %s", unit)
        context.dataStore.edit { prefs ->
            prefs[temperatureUnitKey] = unit.name
        }
    }

    suspend fun setClockFormat(format: ClockFormat) {
        Timber.d("Setting clock format to %s", format)
        context.dataStore.edit { prefs ->
            prefs[clockFormatKey] = format.name
        }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val value = prefs[themeModeKey] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(value)
        } catch (_: IllegalArgumentException) {
            Timber.w("Invalid theme mode preference: %s, falling back to SYSTEM", value)
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        Timber.d("Setting theme mode to %s", mode)
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}
