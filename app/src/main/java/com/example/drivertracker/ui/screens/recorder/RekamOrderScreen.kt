package com.example.drivertracker.ui.screens.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.location.Location
import com.example.drivertracker.data.repository.TrackingState
import com.example.drivertracker.ui.MainViewModel
import com.example.drivertracker.ui.components.SaveOrderDialog
import com.example.drivertracker.ui.utils.toRupiahString
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RekamOrderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val currentAddress by viewModel.currentAddress.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val jarakKePickup by viewModel.jarakKePickup.collectAsStateWithLifecycle()
    val jarakKeTujuan by viewModel.jarakKeTujuan.collectAsStateWithLifecycle()
    val totalJarak by viewModel.totalJarak.collectAsStateWithLifecycle()
    val gpsPoints by viewModel.gpsPoints.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val konsumsiBbm by viewModel.konsumsiBbm.collectAsStateWithLifecycle()
    val hargaBensin by viewModel.hargaBensin.collectAsStateWithLifecycle()
    val isEstimasiBensinAktif by viewModel.isEstimasiBensinAktif.collectAsStateWithLifecycle()

    val todayNetIncome by viewModel.todayNetIncome.collectAsStateWithLifecycle()
    val todayOrderCount by viewModel.todayOrderCount.collectAsStateWithLifecycle()
    val todayTotalDistance by viewModel.todayTotalDistance.collectAsStateWithLifecycle()
    val todayPickupDistance by viewModel.todayPickupDistance.collectAsStateWithLifecycle()
    val todayDeliveryDistance by viewModel.todayDeliveryDistance.collectAsStateWithLifecycle()

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val notificationPermissionState = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else emptyList()
    )
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasLocationPermission, notificationPermissionState.allPermissionsGranted) {
        if (hasLocationPermission) {
            viewModel.startLocationUpdates()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !notificationPermissionState.allPermissionsGranted
            ) {
                notificationPermissionState.launchMultiplePermissionRequest()
            }
        } else {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isSavingOrder by remember { mutableStateOf(false) }

    val shouldOpenSaveDialog by viewModel.shouldOpenSaveDialog.collectAsStateWithLifecycle()
    LaunchedEffect(shouldOpenSaveDialog) {
        if (shouldOpenSaveDialog) {
            showSaveDialog = true
            viewModel.dismissSaveDialog()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsingAlpha"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {

            BottomBarControl(
                trackingState = trackingState,
                onStartOrder = {
                    if (hasLocationPermission) {
                        viewModel.startOrder(context)
                    } else {
                        locationPermissionState.launchMultiplePermissionRequest()
                    }
                },
                onConfirmPickup = { viewModel.confirmPickup(context) },
                onCompleteOrder = { showSaveDialog = true },
                onCancelOrder = { showCancelDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            HeaderBar(
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode(it) }
            )

            TodaySummaryCard(
                netIncome = todayNetIncome,
                orderCount = todayOrderCount,
                totalKm = todayTotalDistance,
                pickupKm = todayPickupDistance,
                deliveryKm = todayDeliveryDistance
            )

            val isGpsHardwareEnabled = remember(currentLocation) {
                val locMgr = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                locMgr?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                locMgr?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
            }

            if (!isGpsHardwareEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.LocationOff, contentDescription = null, tint = Color(0xFFFF9800))
                            Text(
                                text = "Layanan GPS perangkat belum aktif",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("Nyalakan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            GpsTelemetryCard(
                location = currentLocation,
                addressText = currentAddress,
                gpsPointsCount = gpsPoints.size,
                trackingState = trackingState,
                pulsingAlpha = pulsingAlpha
            )

            if (trackingState != TrackingState.IDLE) {
                LiveTrackingPanel(
                    trackingState = trackingState,
                    durationSeconds = durationSeconds,
                    totalJarak = totalJarak,
                    jarakKePickup = jarakKePickup,
                    jarakKeTujuan = jarakKeTujuan,
                    pulsingAlpha = pulsingAlpha
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSaveDialog) {
        SaveOrderDialog(
            jarakKePickup = jarakKePickup,
            jarakKeTujuan = jarakKeTujuan,
            totalJarak = totalJarak,
            durasiSeconds = durationSeconds,
            konsumsiBbm = konsumsiBbm,
            hargaBensin = hargaBensin,
            isEstimasiBensinAktif = isEstimasiBensinAktif,
            isSaving = isSavingOrder,
            onDismiss = { if (!isSavingOrder) showSaveDialog = false },
            onSaveOrder = { jenisOrder, pendapatanKotor, catatan, biayaBensin ->
                isSavingOrder = true
                viewModel.saveAndCompleteOrder(
                    context = context,
                    jenisOrder = jenisOrder,
                    pendapatanKotor = pendapatanKotor,
                    catatan = catatan,
                    biayaBensin = biayaBensin,
                    onComplete = { errorMessage ->
                        isSavingOrder = false
                        if (errorMessage == null) {
                            showSaveDialog = false
                        } else {
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Batalkan pesanan?", fontWeight = FontWeight.Bold) },
            text = { Text("Pencatatan perjalanan ini akan dihentikan dan datanya tidak disimpan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOrder(context)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE2737))
                ) {
                    Text("Batalkan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Kembali")
                }
            }
        )
    }
}

@Composable
private fun HeaderBar(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = Color(0xFF00AA13).copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = null,
                    tint = Color(0xFF00AA13),
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Perekam pesanan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Catat perjalanan dan penghasilan",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                contentDescription = "Theme",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Switch(
                checked = isDarkMode,
                onCheckedChange = onToggleDarkMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00AA13),
                    checkedTrackColor = Color(0xFF00AA13).copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun TodaySummaryCard(
    netIncome: Double,
    orderCount: Int,
    totalKm: Double,
    pickupKm: Double,
    deliveryKm: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFF00AA13).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val orderBadge: @Composable () -> Unit = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00AA13).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$orderCount pesanan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00AA13),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (maxWidth < 320.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pendapatan Hari Ini", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            orderBadge()
                        }
                        Text(
                            text = netIncome.toRupiahString(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00AA13),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Hari Ini:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = netIncome.toRupiahString(),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00AA13),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        orderBadge()
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f km", totalKm),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(text = "•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Jemput:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f km", pickupKm),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00AA13)
                    )
                }

                Text(text = "•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Antar:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f km", deliveryKm),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEE2737)
                    )
                }
            }
        }
    }
}

@Composable
private fun GpsTelemetryCard(
    location: Location?,
    addressText: String,
    gpsPointsCount: Int,
    trackingState: TrackingState,
    pulsingAlpha: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(if (location != null) pulsingAlpha else 0.4f)
                            .background(
                                color = if (location != null) Color(0xFF00AA13) else Color(0xFFFDB813),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (location != null) "GPS AKTIF" else "MENCARI GPS...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (location != null) Color(0xFF00AA13) else Color(0xFFFDB813)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val accText = if (location != null && location.hasAccuracy()) {
                            "±${location.accuracy.toInt()} m"
                        } else {
                            "Mencari..."
                        }
                        Text(
                            text = accText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00AA13).copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Lokasi",
                            tint = Color(0xFF00AA13),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lokasi Terkini",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = addressText.ifBlank { "Menunggu koordinat GPS..." },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrowLayout = maxWidth < 300.dp
            Column(verticalArrangement = Arrangement.spacedBy(if (narrowLayout) 10.dp else 0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                val speedKmh = if (location != null && location.hasSpeed()) {
                    (location.speed * 3.6f)
                } else 0f
                TelemetryMetricItem(
                    label = "Kecepatan",
                    value = String.format(Locale.US, "%.0f km/j", speedKmh),
                    icon = Icons.Rounded.Speed,
                    tint = Color(0xFF00AA13),
                    modifier = Modifier.weight(if (narrowLayout) 1f else 1f)
                )

                val coordText = if (location != null) {
                    String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
                } else {
                    "- , -"
                }
                TelemetryMetricItem(
                    label = "Koordinat",
                    value = coordText,
                    icon = Icons.Rounded.Navigation,
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.weight(if (narrowLayout) 1.4f else 1.3f)
                )

                val titikText = if (trackingState != TrackingState.IDLE) {
                    "$gpsPointsCount titik"
                } else {
                    "Siaga"
                }
                if (!narrowLayout) TelemetryMetricItem(
                    label = "Status Rekam",
                    value = titikText,
                    icon = Icons.Rounded.Timeline,
                    tint = if (trackingState != TrackingState.IDLE) Color(0xFFFDB813) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            if (narrowLayout) {
                val titikText = if (trackingState != TrackingState.IDLE) "$gpsPointsCount titik" else "Siaga"
                TelemetryMetricItem(
                    label = "Status Rekam",
                    value = titikText,
                    icon = Icons.Rounded.Timeline,
                    tint = if (trackingState != TrackingState.IDLE) Color(0xFFFDB813) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LiveTrackingPanel(
    trackingState: TrackingState,
    durationSeconds: Long,
    totalJarak: Double,
    jarakKePickup: Double,
    jarakKeTujuan: Double,
    pulsingAlpha: Float
) {
    val statusColor = if (trackingState == TrackingState.STARTED) Color(0xFFFDB813) else Color(0xFFEE2737)
    val statusText = if (trackingState == TrackingState.STARTED) "Menuju lokasi jemput" else "Mengantar pesanan"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(pulsingAlpha)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Durasi pesanan",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val h = durationSeconds / 3600
                    val m = (durationSeconds % 3600) / 60
                    val s = durationSeconds % 60
                    val timerStr = if (h > 0) {
                        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                    } else {
                        String.format(Locale.US, "%02d:%02d", m, s)
                    }
                    Text(
                        text = timerStr,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Jarak",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f KM", totalJarak),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Jemput: %.1f km", jarakKePickup),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00AA13)
                )
                Text(
                    text = String.format(Locale.US, "Antar: %.1f km", jarakKeTujuan),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEE2737)
                )
            }
        }
    }
}

@Composable
private fun BottomBarControl(
    trackingState: TrackingState,
    onStartOrder: () -> Unit,
    onConfirmPickup: () -> Unit,
    onCompleteOrder: () -> Unit,
    onCancelOrder: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (trackingState) {
                TrackingState.IDLE -> {
                    Button(
                        onClick = onStartOrder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00AA13)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mulai pencatatan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                TrackingState.STARTED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelOrder,
                            modifier = Modifier
                                .weight(0.35f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEE2737)
                            )
                        ) {
                            Text("Batalkan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = onConfirmPickup,
                            modifier = Modifier
                                .weight(0.65f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFDB813)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Jemput",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
                TrackingState.PICKED_UP -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelOrder,
                            modifier = Modifier
                                .weight(0.35f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEE2737)
                            )
                        ) {
                            Text("Batalkan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = onCompleteOrder,
                            modifier = Modifier
                                .weight(0.65f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEE2737)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Selesai",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
