package com.example.drivertracker.ui.screens.radar

import android.content.Context
import android.Manifest
import android.location.Location
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivertracker.ml.RadarOnnxManager
import com.example.drivertracker.ml.RadarPrediction
import com.example.drivertracker.ui.MainViewModel
import com.example.drivertracker.ui.components.DRIVER_TRACKER_OSM_TILE_SOURCE
import com.example.drivertracker.ui.components.MapPinGlyph
import com.example.drivertracker.ui.components.createMapPinDrawable
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SmartHeatmapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) viewModel.startLocationUpdates()
        else locationPermissions.launchMultiplePermissionRequest()
    }

    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val modelReady by produceState(initialValue = false) {
        value = withContext(Dispatchers.IO) { RadarOnnxManager.hasModel(context) }
    }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var shouldRecenter by remember { mutableStateOf(false) }
    var forecastHourRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = java.util.Calendar.getInstance()
            val millisToNextHour = (
                3_600_000L - now.get(java.util.Calendar.MINUTE) * 60_000L -
                    now.get(java.util.Calendar.SECOND) * 1_000L - now.get(java.util.Calendar.MILLISECOND)
                ).coerceAtLeast(1_000L)
            delay(millisToNextHour)
            forecastHourRefresh++
        }
    }

    val predictions by produceState(
        initialValue = emptyList<RadarPrediction>(),
        currentLocation?.latitude?.let { (it * 1000).toInt() },
        currentLocation?.longitude?.let { (it * 1000).toInt() },
        selectedCategory,
        modelReady,
        forecastHourRefresh
    ) {
        value = withContext(Dispatchers.IO) {
            val driver = currentLocation ?: return@withContext emptyList()
            if (!modelReady) return@withContext emptyList()
            val categories = if (selectedCategory == "Semua") {
                listOf("Penumpang", "Makanan", "Paket")
            } else listOf(selectedCategory)
            val startTime = LocalDateTime.now()
            val currentPredictions = RadarOnnxManager.predictAround(
                context = context,
                latitude = driver.latitude,
                longitude = driver.longitude,
                forecastTime = startTime,
                categories = categories
            )
            val nextHourPredictions = RadarOnnxManager.predictAround(
                context = context,
                latitude = driver.latitude,
                longitude = driver.longitude,
                forecastTime = startTime.plusHours(1),
                categories = categories
            )

            (currentPredictions + nextHourPredictions)
                .groupBy { Triple(it.latitude, it.longitude, it.category) }
                .map { (key, values) ->
                    RadarPrediction(
                        latitude = key.first,
                        longitude = key.second,
                        category = key.third,
                        predictedOrders = values.map { it.predictedOrders }.average().toFloat(),
                        forecastTime = startTime
                    )
                }
                .sortedByDescending { it.predictedOrders }
                .take(30)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RadarMapView(
            currentLocation = currentLocation,
            predictions = predictions,
            isDarkMode = isDarkMode,
            recenterTrigger = shouldRecenter,
            onRecenterHandled = { shouldRecenter = false }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Radar pintar",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background((if (currentLocation != null) Color(0xFF00AA13) else Color.Gray).copy(alpha = pulsingAlpha))
                        )
                        Text(
                            text = if (currentLocation != null) "GPS aktif" else "Menunggu GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentLocation != null) Color(0xFF00AA13) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 2.dp
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    items(listOf("Semua", "Penumpang", "Makanan", "Paket")) { category ->
                        val categoryColor = when (category) {
                            "Makanan" -> Color(0xFFDC2626)
                            "Paket" -> Color(0xFF2563EB)
                            else -> Color(0xFF16A34A)
                        }
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 11.sp, maxLines = 1) },
                            leadingIcon = if (selectedCategory == category) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = categoryColor,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = categoryColor.copy(alpha = 0.08f),
                                labelColor = categoryColor,
                                iconColor = categoryColor
                            ),
                            border = null
                        )
                    }
                }
            }

        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .clickable { shouldRecenter = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 5.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Pusatkan peta",
                    tint = Color(0xFF00AA13),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun RadarMapView(
    currentLocation: Location?,
    predictions: List<RadarPrediction> = emptyList(),
    isDarkMode: Boolean,
    recenterTrigger: Boolean = false,
    onRecenterHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_radar_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "DriverTracker/1.0 (+app-id:${context.packageName})"
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.0)
            setTileSource(DRIVER_TRACKER_OSM_TILE_SOURCE)
        }
    }
    var hasCenteredOnDriver by remember(mapView) { mutableStateOf(false) }

    LaunchedEffect(currentLocation) {
        if (!hasCenteredOnDriver) {
            currentLocation?.let { location ->
                mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                hasCenteredOnDriver = true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(isDarkMode) {
        mapView.setTileSource(DRIVER_TRACKER_OSM_TILE_SOURCE)
        mapView.overlayManager.tilesOverlay.setColorFilter(null)
        mapView.invalidate()
    }

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger) {
            currentLocation?.let { loc ->
                mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                mapView.controller.setZoom(15.0)
                hasCenteredOnDriver = true
            }
            onRecenterHandled()
        }
    }

    val predictionMarkers = remember(mapView) { mutableListOf<Marker>() }
    val driverMarker = remember(mapView) {
        Marker(mapView).apply {
            icon = createMapPinDrawable(context, android.graphics.Color.rgb(8, 145, 178), MapPinGlyph.DRIVER)
            title = "Posisi Anda"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }
    val driverAccuracyCircle = remember(mapView) {
        Polygon(mapView).apply {
            fillPaint.color = android.graphics.Color.parseColor("#3300AA13")
            outlinePaint.color = android.graphics.Color.parseColor("#8000AA13")
            outlinePaint.strokeWidth = 2f
        }
    }

    LaunchedEffect(predictions) {
        mapView.overlays.removeAll(predictionMarkers)
        predictionMarkers.clear()
        predictions.forEach { prediction ->
            val markerColor = when (prediction.category) {
                "Makanan" -> android.graphics.Color.rgb(220, 38, 38)
                "Paket" -> android.graphics.Color.rgb(37, 99, 235)
                else -> android.graphics.Color.rgb(22, 163, 74)
            }
            val marker = Marker(mapView).apply {
                position = GeoPoint(prediction.latitude, prediction.longitude)
                icon = createMapPinDrawable(context, markerColor, MapPinGlyph.PREDICTION)
                title = "${prediction.category} • Prediksi 1 jam"
                snippet = "Rata-rata: ${String.format(java.util.Locale.getDefault(), "%.1f", prediction.predictedOrders)} order/jam"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            predictionMarkers += marker
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            val driverGeo = GeoPoint(loc.latitude, loc.longitude)

            if (loc.hasAccuracy() && loc.accuracy > 0) {
                driverAccuracyCircle.points = Polygon.pointsAsCircle(driverGeo, loc.accuracy.toDouble())
                if (!mapView.overlays.contains(driverAccuracyCircle)) {
                    mapView.overlays.add(driverAccuracyCircle)
                }
            } else {
                mapView.overlays.remove(driverAccuracyCircle)
            }

            driverMarker.position = driverGeo
            if (!mapView.overlays.contains(driverMarker)) {
                mapView.overlays.add(driverMarker)
            }

            if (!hasCenteredOnDriver) {
                mapView.controller.animateTo(driverGeo)
                hasCenteredOnDriver = true
            }
        } ?: run {
            mapView.overlays.remove(driverAccuracyCircle)
            mapView.overlays.remove(driverMarker)
        }

        mapView.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            color = Color.White.copy(alpha = 0.88f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                color = Color(0xFF263238),
                fontSize = 10.sp
            )
        }
    }
}
