package com.zephyrus.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyrus.app.domain.model.ClockFormat
import com.zephyrus.app.domain.model.TemperatureUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        // Temperature Unit
        Text(
            text = "Temperature Unit",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            TemperatureUnit.entries.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = uiState.temperatureUnit == unit,
                    onClick = { viewModel.setTemperatureUnit(unit) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TemperatureUnit.entries.size,
                    ),
                ) {
                    Text(
                        when (unit) {
                            TemperatureUnit.FAHRENHEIT -> "Fahrenheit (°F)"
                            TemperatureUnit.CELSIUS -> "Celsius (°C)"
                        },
                    )
                }
            }
        }

        // Clock Format
        Text(
            text = "Clock Format",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            ClockFormat.entries.forEachIndexed { index, format ->
                SegmentedButton(
                    selected = uiState.clockFormat == format,
                    onClick = { viewModel.setClockFormat(format) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ClockFormat.entries.size,
                    ),
                ) {
                    Text(format.label())
                }
            }
        }
    }
}
