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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivertracker.data.repository.TrackingState
import com.example.drivertracker.ui.MainViewModel
import com.example.drivertracker.ui.components.SaveOrderDialog
import com.example.drivertracker.ui.components.DriverTrackerScaffold
import com.example.drivertracker.ui.components.pageContentPadding
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
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val jarakKePickup by viewModel.jarakKePickup.collectAsStateWithLifecycle()
    val jarakKeTujuan by viewModel.jarakKeTujuan.collectAsStateWithLifecycle()
    val totalJarak by viewModel.totalJarak.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val konsumsiBbm by viewModel.konsumsiBbm.collectAsStateWithLifecycle()
    val hargaBensin by viewModel.hargaBensin.collectAsStateWithLifecycle()
    val isEstimasiBensinAktif by viewModel.isEstimasiBensinAktif.collectAsStateWithLifecycle()

    val todayNetIncome by viewModel.todayNetIncome.collectAsStateWithLifecycle()
    val todayOrderCount by viewModel.todayOrderCount.collectAsStateWithLifecycle()
    val todayTotalDistance by viewModel.todayTotalDistance.collectAsStateWithLifecycle()
    val todayAverageSpeed by viewModel.todayAverageSpeed.collectAsStateWithLifecycle()
    val todayTotalDuration by viewModel.todayTotalDuration.collectAsStateWithLifecycle()

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

    DriverTrackerScaffold(
        title = "Perekam pesanan",
        modifier = modifier,
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    contentDescription = if (isDarkMode) "Mode gelap" else "Mode terang",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(36.dp)
                        .toggleable(
                            value = isDarkMode,
                            role = Role.Switch,
                            onValueChange = viewModel::toggleDarkMode
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(26.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF00AA13) else MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(26.dp)
                            .padding(horizontal = 3.dp),
                        contentAlignment = if (isDarkMode) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        },
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
                .pageContentPadding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TodaySummaryCard(
                netIncome = todayNetIncome,
                orderCount = todayOrderCount,
                totalKm = todayTotalDistance,
                averageSpeedKmh = todayAverageSpeed,
                totalDurationSeconds = todayTotalDuration
            )

            val isGpsHardwareEnabled = remember(currentLocation) {
                val locMgr = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                locMgr?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                locMgr?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
            }

            LiveTrackingPanel(
                trackingState = trackingState,
                durationSeconds = durationSeconds,
                totalJarak = totalJarak,
                jarakKePickup = jarakKePickup,
                jarakKeTujuan = jarakKeTujuan,
                pulsingAlpha = pulsingAlpha
            )

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
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Batalkan pesanan?", fontWeight = FontWeight.Bold) },
            text = { Text("Pencatatan perjalanan ini akan dihentikan dan datanya tidak disimpan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOrder(context)
                        showCancelDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Batalkan")
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = { showCancelDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Kembali")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodaySummaryCard(
    netIncome: Double,
    orderCount: Int,
    totalKm: Double,
    averageSpeedKmh: Double,
    totalDurationSeconds: Long
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
                        text = "Rata-rata:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f km/j", averageSpeedKmh),
                        fontSize = 11.sp,
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
                        text = "Durasi:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatSummaryDuration(totalDurationSeconds),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun formatSummaryDuration(totalSeconds: Long): String {
    val totalMinutes = totalSeconds.coerceAtLeast(0L) / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}j ${minutes}m" else "${minutes}m"
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
    val statusColor = when (trackingState) {
        TrackingState.IDLE -> Color(0xFF00AA13)
        TrackingState.STARTED -> Color(0xFFFDB813)
        else -> Color(0xFFEE2737)
    }
    val statusText = when (trackingState) {
        TrackingState.IDLE -> "Siap mencatat"
        TrackingState.STARTED -> "Menuju lokasi jemput"
        else -> "Mengantar pesanan"
    }

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
                        .alpha(if (trackingState == TrackingState.IDLE) 1f else pulsingAlpha)
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

            if (trackingState == TrackingState.IDLE) {
                Text(
                    text = "Tekan Mulai pencatatan untuk merekam perjalanan.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
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
