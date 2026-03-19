package com.zephyrus.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** 5-level glyph indicator for UV Index (WHO standard scale). */
@Composable
fun UvIndexGlyph(uvIndex: Double, modifier: Modifier = Modifier) {
    val (icon, label, color) = uvIndexLevel(uvIndex)
    GlyphRow(icon = icon, label = label, color = color, modifier = modifier)
}

/** 5-level glyph indicator for barometric pressure. */
@Composable
fun PressureGlyph(pressure: Double, modifier: Modifier = Modifier) {
    val (icon, label, color) = pressureLevel(pressure)
    GlyphRow(icon = icon, label = label, color = color, modifier = modifier)
}

@Composable
private fun GlyphRow(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private data class GlyphLevel(
    val icon: ImageVector,
    val label: String,
    val color: Color,
)

private fun uvIndexLevel(uvIndex: Double): GlyphLevel = when {
    uvIndex == 0.0 -> GlyphLevel(Icons.Filled.DarkMode, "None", Color(0xFF42A5F5))
    uvIndex < 3 -> GlyphLevel(Icons.Filled.WbTwilight, "Low", Color(0xFF4CAF50))
    uvIndex < 6 -> GlyphLevel(Icons.Filled.WbSunny, "Moderate", Color(0xFFFFC107))
    uvIndex < 8 -> GlyphLevel(Icons.Filled.WbSunny, "High", Color(0xFFFF9800))
    uvIndex < 11 -> GlyphLevel(Icons.Filled.WbSunny, "Very High", Color(0xFFE53935))
    else -> GlyphLevel(Icons.Filled.Warning, "Extreme", Color(0xFF7B1FA2))
}

private fun pressureLevel(pressure: Double): GlyphLevel = when {
    pressure < 1000 -> GlyphLevel(Icons.Filled.Thunderstorm, "Very Low", Color(0xFF7B1FA2))
    pressure < 1010 -> GlyphLevel(Icons.Filled.Cloud, "Low", Color(0xFF42A5F5))
    pressure < 1020 -> GlyphLevel(Icons.Filled.CheckCircle, "Normal", Color(0xFF66BB6A))
    pressure <= 1030 -> GlyphLevel(Icons.Filled.WbSunny, "High", Color(0xFFFFA726))
    else -> GlyphLevel(Icons.Filled.WbSunny, "Very High", Color(0xFFD32F2F))
}
