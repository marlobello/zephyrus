package com.zephyrus.app.ui.maps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Grain
import androidx.compose.ui.graphics.vector.ImageVector

enum class MapLayer(val label: String, val icon: ImageVector) {
    TEMPERATURE("Temp", Icons.Filled.Thermostat),
    HUMIDITY("Humid", Icons.Filled.WaterDrop),
    PRESSURE("Press", Icons.Filled.Speed),
    PRECIPITATION("Precip", Icons.Filled.Grain),
}
