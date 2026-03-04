package com.zephyrus.app.ui.maps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zephyrus.app.domain.model.TemperatureUnit
import com.zephyrus.app.ui.components.ZephyrusTopAppBar
import com.zephyrus.app.ui.theme.HumidityDry
import com.zephyrus.app.ui.theme.HumidityHigh
import com.zephyrus.app.ui.theme.HumidityLow
import com.zephyrus.app.ui.theme.HumidityMedium
import com.zephyrus.app.ui.theme.HumiditySaturated
import com.zephyrus.app.ui.theme.PrecipExtreme
import com.zephyrus.app.ui.theme.PrecipHeavy
import com.zephyrus.app.ui.theme.PrecipLight
import com.zephyrus.app.ui.theme.PrecipModerate
import com.zephyrus.app.ui.theme.PrecipNone
import com.zephyrus.app.ui.theme.PressureAboveAvg
import com.zephyrus.app.ui.theme.PressureBelowAvg
import com.zephyrus.app.ui.theme.PressureHigh
import com.zephyrus.app.ui.theme.PressureLow
import com.zephyrus.app.ui.theme.PressureNormal
import com.zephyrus.app.ui.theme.TempBlue
import com.zephyrus.app.ui.theme.TempDeepBlue
import com.zephyrus.app.ui.theme.TempDeepRed
import com.zephyrus.app.ui.theme.TempLightBlue
import com.zephyrus.app.ui.theme.TempLightRed
import com.zephyrus.app.ui.theme.TempRed
import com.zephyrus.app.ui.theme.TempWhite
import com.zephyrus.app.ui.theme.humidityToArgb
import com.zephyrus.app.ui.theme.precipitationToArgb
import com.zephyrus.app.ui.theme.pressureToArgb
import com.zephyrus.app.ui.theme.temperatureToArgb
import com.zephyrus.app.ui.theme.toFahrenheit
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.GroundOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    locationName: String = "Zephyrus",
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: MapsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasSetInitialCenter by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ZephyrusTopAppBar(
            locationName = locationName,
            subtitle = "Weather Map",
            onRefreshClick = { viewModel.refresh() },
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
        )

        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            val mapView = remember {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(8.0)
                    setMaxZoomLevel(18.0)
                    // Debounced listener — fires after 500ms of inactivity
                    addMapListener(DelayedMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val bb = boundingBox
                            viewModel.onViewportChanged(
                                mapCenter.latitude,
                                mapCenter.longitude,
                                zoomLevelDouble,
                                visibleLatSpan = bb.latNorth - bb.latSouth,
                                visibleLonSpan = bb.lonEast - bb.lonWest,
                            )
                            return true
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            val bb = boundingBox
                            viewModel.onViewportChanged(
                                mapCenter.latitude,
                                mapCenter.longitude,
                                zoomLevelDouble,
                                visibleLatSpan = bb.latNorth - bb.latSouth,
                                visibleLonSpan = bb.lonEast - bb.lonWest,
                            )
                            return true
                        }
                    }, 500))
                }
            }

            // MapView lifecycle management
            DisposableEffect(mapView) {
                mapView.onResume()
                onDispose {
                    // Recycle any remaining overlay bitmaps
                    mapView.overlays.filterIsInstance<GroundOverlay>().forEach { it.image?.recycle() }
                    mapView.onPause()
                    mapView.onDetach()
                }
            }

            // Set initial center when location becomes available
            LaunchedEffect(latitude, longitude) {
                if ((latitude != 0.0 || longitude != 0.0) && !hasSetInitialCenter) {
                    mapView.controller.setCenter(GeoPoint(latitude, longitude))
                    hasSetInitialCenter = true
                    // Wait for layout so boundingBox is accurate
                    kotlinx.coroutines.delay(300)
                    val bb = mapView.boundingBox
                    val visibleLatSpan = bb.latNorth - bb.latSouth
                    val visibleLonSpan = bb.lonEast - bb.lonWest
                    val visibleSpan = maxOf(visibleLatSpan, visibleLonSpan)

                    // Calculate min zoom: overlay max diameter is 20° (radius 10° × 2)
                    // Each zoom level halves the visible span, so:
                    // minZoom = currentZoom - log2(maxDiameter / currentSpan)
                    val maxOverlayDiameter = 20.0 // 2 * MAX_RADIUS (10.0°)
                    if (visibleSpan > 0) {
                        val currentZoom = mapView.zoomLevelDouble
                        val minZoom = currentZoom - kotlin.math.ln(maxOverlayDiameter / visibleSpan) / kotlin.math.ln(2.0)
                        mapView.setMinZoomLevel(minZoom.coerceAtLeast(2.0))
                    }

                    viewModel.onViewportChanged(
                        latitude, longitude, mapView.zoomLevelDouble,
                        visibleLatSpan = visibleLatSpan,
                        visibleLonSpan = visibleLonSpan,
                    )
                }
            }

            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    val grid = when (uiState.activeLayer) {
                        MapLayer.TEMPERATURE -> uiState.gridTemperatures
                        MapLayer.HUMIDITY -> uiState.gridHumidity
                        MapLayer.PRESSURE -> uiState.gridPressure
                        MapLayer.PRECIPITATION -> uiState.gridPrecipitation
                    }
                    if (grid != null && grid.isNotEmpty() && grid[0].isNotEmpty()) {
                        // Recycle old overlay bitmaps before removing
                        map.overlays.filterIsInstance<GroundOverlay>().forEach { old ->
                            old.image?.recycle()
                        }
                        map.overlays.removeAll { it is GroundOverlay }

                        val overlay = createWeatherOverlay(
                            grid = grid,
                            centerLat = uiState.centerLatitude,
                            centerLon = uiState.centerLongitude,
                            radiusDeg = uiState.radiusDegrees,
                            layer = uiState.activeLayer,
                            unit = uiState.temperatureUnit,
                        )
                        map.overlays.add(overlay)
                        map.invalidate()
                    }
                },
            )

            // Layer selector — segmented button row
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                MapLayer.entries.forEachIndexed { index, layer ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, MapLayer.entries.size),
                        onClick = { viewModel.setActiveLayer(layer) },
                        selected = uiState.activeLayer == layer,
                        colors = SegmentedButtonDefaults.colors(
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        icon = {
                            Icon(
                                imageVector = layer.icon,
                                contentDescription = layer.label,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = { Text(layer.label, fontSize = 11.sp) },
                    )
                }
            }

            // Color legend
            ColorLegend(
                layer = uiState.activeLayer,
                unit = uiState.temperatureUnit,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

private fun createWeatherOverlay(
    grid: Array<DoubleArray>,
    centerLat: Double,
    centerLon: Double,
    radiusDeg: Double,
    layer: MapLayer,
    unit: TemperatureUnit,
): GroundOverlay {
    val rows = grid.size
    val cols = grid[0].size
    val scaleFactor = 40
    val bmpWidth = (cols * scaleFactor).coerceAtLeast(1)
    val bmpHeight = (rows * scaleFactor).coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    // Need at least 2 rows/cols for bilinear interpolation
    val maxY0 = (rows - 2).coerceAtLeast(0)
    val maxX0 = (cols - 2).coerceAtLeast(0)

    for (py in 0 until bmpHeight) {
        for (px in 0 until bmpWidth) {
            val gx = px.toFloat() / scaleFactor
            // Flip vertically: bitmap top (py=0) → grid bottom (south),
            // because GroundOverlay maps bitmap origin to SW corner
            val gy = (bmpHeight - 1 - py).toFloat() / scaleFactor
            val x0 = gx.toInt().coerceIn(0, maxX0)
            val y0 = gy.toInt().coerceIn(0, maxY0)
            val fx = if (maxX0 > 0) gx - x0 else 0f
            val fy = if (maxY0 > 0) gy - y0 else 0f
            val x1 = (x0 + 1).coerceAtMost(cols - 1)
            val y1 = (y0 + 1).coerceAtMost(rows - 1)
            val value = (1 - fx) * (1 - fy) * grid[y0][x0] +
                fx * (1 - fy) * grid[y0][x1] +
                (1 - fx) * fy * grid[y1][x0] +
                fx * fy * grid[y1][x1]

            paint.color = when (layer) {
                MapLayer.TEMPERATURE -> temperatureToArgb(toFahrenheit(value, unit), alpha = 120)
                MapLayer.HUMIDITY -> humidityToArgb(value.toFloat(), alpha = 120)
                MapLayer.PRESSURE -> pressureToArgb(value.toFloat(), alpha = 120)
                MapLayer.PRECIPITATION -> precipitationToArgb(value.toFloat(), alpha = 120)
            }
            canvas.drawPoint(px.toFloat(), py.toFloat(), paint)
        }
    }

    val sw = GeoPoint(centerLat - radiusDeg, centerLon - radiusDeg)
    val ne = GeoPoint(centerLat + radiusDeg, centerLon + radiusDeg)

    return GroundOverlay().apply {
        setImage(bitmap)
        setPosition(sw, ne)
    }
}

@Composable
private fun ColorLegend(
    layer: MapLayer,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    val (colors, startLabel, endLabel) = when (layer) {
        MapLayer.TEMPERATURE -> {
            val label = if (unit == TemperatureUnit.FAHRENHEIT) "°F" else "°C"
            val low = if (unit == TemperatureUnit.FAHRENHEIT) "0" else "-18"
            val high = if (unit == TemperatureUnit.FAHRENHEIT) "105" else "41"
            Triple(
                listOf(TempDeepBlue, TempBlue, TempLightBlue, TempWhite, TempLightRed, TempRed, TempDeepRed),
                "$low$label",
                "$high$label",
            )
        }
        MapLayer.HUMIDITY -> Triple(
            listOf(HumidityDry, HumidityLow, HumidityMedium, HumidityHigh, HumiditySaturated),
            "0%",
            "100%",
        )
        MapLayer.PRESSURE -> Triple(
            listOf(PressureLow, PressureBelowAvg, PressureNormal, PressureAboveAvg, PressureHigh),
            "980",
            "1040 hPa",
        )
        MapLayer.PRECIPITATION -> Triple(
            listOf(PrecipNone, PrecipLight, PrecipModerate, PrecipHeavy, PrecipExtreme),
            "0",
            "0.5+ in/h",
        )
    }

    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(colors)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(startLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(endLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
