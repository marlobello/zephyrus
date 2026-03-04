package com.zephyrus.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

enum class ZephyrusScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    Current("current", "Current", Icons.Filled.AccessTime),
    Forecast("forecast", "Forecast", Icons.Filled.CalendarMonth),
    Maps("maps", "Maps", Icons.Filled.Map),
}
