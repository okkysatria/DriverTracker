package com.example.drivertracker.ui.screens.radar

import android.content.Context
import android.Manifest
import android.location.Location
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivertracker.data.local.entity.OrderRecord
import com.example.drivertracker.ml.HotspotInfo
import com.example.drivertracker.ml.SmartHeatmapPredictor
import com.example.drivertracker.ui.MainViewModel
import com.example.drivertracker.ui.components.DRIVER_TRACKER_OSM_TILE_SOURCE
import com.example.drivertracker.ui.components.MapPinGlyph
import com.example.drivertracker.ui.components.createMapPinDrawable
import com.example.drivertracker.ui.components.orderMapPinColor
import com.example.drivertracker.ui.components.orderMapPinGlyph
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val onnxModelName by viewModel.customOnnxModelName.collectAsStateWithLifecycle()

    val predictor = remember(context) { SmartHeatmapPredictor(context) }

    val showHistory by viewModel.showRadarHistory.collectAsStateWithLifecycle()
    val showAiRadar by viewModel.showRadarAi.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedSort by remember { mutableStateOf("Terdekat") }
    var targetHotspotPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var shouldRecenter by remember { mutableStateOf(false) }

    val curLat = currentLocation?.latitude ?: 0.0
    val curLng = currentLocation?.longitude ?: 0.0
    val latestRecordedLocation = remember(allOrders) {
        allOrders.firstNotNullOfOrNull { order ->
            when {
                order.latitudePickup != 0.0 && order.longitudePickup != 0.0 ->
                    GeoPoint(order.latitudePickup, order.longitudePickup)
                order.latitudeAwal != 0.0 && order.longitudeAwal != 0.0 ->
                    GeoPoint(order.latitudeAwal, order.longitudeAwal)
                else -> null
            }
        }
    }

    val hotspots = remember(curLat, curLng, allOrders, selectedCategory, selectedSort) {
        predictor.predictHotspots(
            currentLat = curLat,
            currentLng = curLng,
            orders = allOrders,
            categoryFilter = selectedCategory,
            sortBy = selectedSort
        )
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        val mapHeight = (maxHeight * 0.40f).coerceIn(190.dp, 320.dp)

        Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Radar pintar",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFF00AA13).copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radar,
                    contentDescription = null,
                    tint = Color(0xFF00AA13),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                RadarMapView(
                    currentLocation = currentLocation,
                    latestRecordedLocation = latestRecordedLocation,
                    hotspots = if (showAiRadar) hotspots else emptyList(),
                    historyOrders = if (showHistory) allOrders else emptyList(),
                    targetPoint = targetHotspotPoint,
                    isDarkMode = isDarkMode,
                    recenterTrigger = shouldRecenter,
                    onRecenterHandled = { shouldRecenter = false }
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00AA13).copy(alpha = pulsingAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                        text = if (currentLocation != null) "GPS Aktif" else "Menunggu GPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        color = if (currentLocation != null) Color(0xFF00AA13) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(44.dp)
                        .clickable { shouldRecenter = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Pusatkan Peta",
                            tint = Color(0xFF00AA13),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (onnxModelName.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E88E5).copy(alpha = 0.88f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Model aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth()) {

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                val categories = listOf("Semua", "Penumpang", "Makanan", "Paket")
                items(categories) { cat ->
                    val categoryColor = when (cat) {
                        "Penumpang" -> Color(0xFF16A34A)
                        "Makanan" -> Color(0xFFDC2626)
                        "Paket" -> Color(0xFF2563EB)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        leadingIcon = if (selectedCategory == cat) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = categoryColor,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            selectedTrailingIconColor = Color.White,
                            containerColor = categoryColor.copy(alpha = 0.08f),
                            labelColor = categoryColor,
                            iconColor = categoryColor
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    Text(
                        text = "Urutkan berdasarkan:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(listOf("Terdekat", "Paling Berpotensi")) { sortOpt ->
                        AssistChip(
                            onClick = { selectedSort = sortOpt },
                            label = { Text(sortOpt, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (sortOpt == "Terdekat") Icons.Rounded.NearMe else Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selectedSort == sortOpt) Color(0xFF00AA13) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            border = null
                        )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "8 area paling potensial",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text(
            text = "Estimasi dihitung dari riwayat pesanan yang memiliki lokasi.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (hotspots.isEmpty()) {
                item {
                    Text(
                        text = if (currentLocation == null) {
                            "Menunggu lokasi GPS dan riwayat pesanan untuk menghitung area potensial."
                        } else {
                            "Belum ada riwayat pesanan dengan lokasi yang tercatat."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(hotspots, key = { it.name + it.rank }) { hotspot ->
                    HotspotCardItem(
                        hotspot = hotspot,
                        onLihatPetaClick = {
                            targetHotspotPoint = GeoPoint(hotspot.latitude, hotspot.longitude)
                        }
                    )
                }
            }
        }
        }
    }
}

@Composable
fun HotspotCardItem(
    hotspot: HotspotInfo,
    onLihatPetaClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (hotspot.rank <= 3) Color(0xFF00AA13)
                            else MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${hotspot.rank}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hotspot.rank <= 3) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = hotspot.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${hotspot.distanceKm} km • ${hotspot.category}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00AA13).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF00AA13),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Potensi ${hotspot.gacorScore}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00AA13)
                        )
                    }
                }

                Button(
                    onClick = onLihatPetaClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00AA13)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lihat Peta",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun RadarMapView(
    currentLocation: Location?,
    hotspots: List<HotspotInfo>,
    historyOrders: List<OrderRecord>,
    targetPoint: GeoPoint?,
    isDarkMode: Boolean,
    latestRecordedLocation: GeoPoint? = null,
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

    LaunchedEffect(currentLocation, latestRecordedLocation) {
        if (!hasCenteredOnDriver) {
            val recordedLocation = currentLocation?.let { GeoPoint(it.latitude, it.longitude) }
                ?: latestRecordedLocation
            if (recordedLocation != null) {
                mapView.controller.animateTo(recordedLocation)
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

    LaunchedEffect(targetPoint) {
        targetPoint?.let { pt ->
            mapView.controller.animateTo(pt)
            mapView.controller.setZoom(16.5)
        }
    }

    LaunchedEffect(currentLocation, hotspots, historyOrders) {
        mapView.overlays.clear()

        hotspots.forEach { hotspot ->
            if (hotspot.latitude.isFinite() && hotspot.longitude.isFinite()) {

                val centerMarker = Marker(mapView).apply {
                    position = GeoPoint(hotspot.latitude, hotspot.longitude)
                    icon = createMapPinDrawable(context, android.graphics.Color.rgb(124, 58, 237), MapPinGlyph.HOTSPOT)
                    title = hotspot.name
                    snippet = "Potensi ${hotspot.gacorScore} • ${hotspot.orderCountInZone} pesanan • ${hotspot.category}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(centerMarker)
            }
        }

        val orderLocations = historyOrders.mapNotNull { order ->
            val coordinates = listOf(
                order.latitudePickup to order.longitudePickup,
                order.latitudeAwal to order.longitudeAwal,
                order.latitudeAkhir to order.longitudeAkhir
            ).firstOrNull { (lat, lon) ->
                lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 &&
                    lon in -180.0..180.0 && (lat != 0.0 || lon != 0.0)
            } ?: return@mapNotNull null
            order to coordinates
        }

        val groupedOrderLocations = orderLocations.groupBy { (_, coordinates) ->
            (coordinates.first * 10_000).toInt() to (coordinates.second * 10_000).toInt()
        }
        groupedOrderLocations.values.forEach { ordersAtLocation ->
            val (primaryOrder, coordinates) = ordersAtLocation.first()
            val pin = Marker(mapView).apply {
                position = GeoPoint(coordinates.first, coordinates.second)
                icon = createMapPinDrawable(
                    context,
                    orderMapPinColor(primaryOrder.jenisOrder),
                    orderMapPinGlyph(primaryOrder.jenisOrder)
                )
                title = if (ordersAtLocation.size == 1) "Pesanan ${primaryOrder.jenisOrder}"
                else "${ordersAtLocation.size} pesanan di lokasi ini"
                snippet = ordersAtLocation.joinToString("\n") { (order, _) ->
                    val orderTime = listOf(order.jamSelesai, order.jamPickup, order.jamMulai)
                        .firstOrNull { it > 0L }
                        ?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) }
                        ?: "Jam tidak tersedia"
                    val address = order.alamatPickup.ifBlank {
                        order.alamatAwal.ifBlank { order.alamatAkhir }
                    }
                    buildString {
                        append("${order.jenisOrder} • $orderTime")
                        if (address.isNotBlank()) append(" • $address")
                        if (order.tanggal.isNotBlank()) append(" • ${order.tanggal}")
                    }
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(pin)
        }

        currentLocation?.let { loc ->
            val driverGeo = GeoPoint(loc.latitude, loc.longitude)

            if (loc.hasAccuracy() && loc.accuracy > 0) {
                val circle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(driverGeo, loc.accuracy.toDouble())
                    fillPaint.color = android.graphics.Color.parseColor("#3300AA13")
                    outlinePaint.color = android.graphics.Color.parseColor("#8000AA13")
                    outlinePaint.strokeWidth = 2f
                }
                mapView.overlays.add(circle)
            }

            val driverMarker = Marker(mapView).apply {
                position = driverGeo
                icon = createMapPinDrawable(context, android.graphics.Color.rgb(8, 145, 178), MapPinGlyph.DRIVER)
                title = "Posisi Anda"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(driverMarker)

            if (targetPoint == null && !hasCenteredOnDriver) {
                mapView.controller.animateTo(driverGeo)
                hasCenteredOnDriver = true
            }
        }

        mapView.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
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
