package com.example.drivertracker.ui.screens.routeposter

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivertracker.data.local.entity.OrderRecord
import com.example.drivertracker.data.model.GpsPoint
import com.example.drivertracker.ui.MainViewModel
import com.example.drivertracker.ui.utils.toRupiahString
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackPosterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val liveGpsPoints by viewModel.gpsPoints.collectAsStateWithLifecycle()
    val todayNetIncome by viewModel.todayNetIncome.collectAsStateWithLifecycle()
    val todayOrderCount by viewModel.todayOrderCount.collectAsStateWithLifecycle()
    val todayTotalDistance by viewModel.todayTotalDistance.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val driverName by viewModel.driverName.collectAsStateWithLifecycle()

    val posterShowAppName by viewModel.posterShowAppName.collectAsStateWithLifecycle()
    val posterShowDriverName by viewModel.posterShowDriverName.collectAsStateWithLifecycle()
    val posterShowDistance by viewModel.posterShowDistance.collectAsStateWithLifecycle()
    val posterShowIncome by viewModel.posterShowIncome.collectAsStateWithLifecycle()
    val posterShowRouteLine by viewModel.posterShowRouteLine.collectAsStateWithLifecycle()
    val posterShowDuration by viewModel.posterShowDuration.collectAsStateWithLifecycle()
    val posterShowAvgSpeed by viewModel.posterShowAvgSpeed.collectAsStateWithLifecycle()
    val posterShowMaxSpeed by viewModel.posterShowMaxSpeed.collectAsStateWithLifecycle()
    val posterRouteLineColor by viewModel.posterRouteLineColor.collectAsStateWithLifecycle()

    val elementVisibility = remember(
        posterShowAppName, posterShowDriverName, posterShowDistance,
        posterShowIncome, posterShowRouteLine, posterShowDuration,
        posterShowAvgSpeed, posterShowMaxSpeed
    ) {
        PosterElementVisibility(
            showAppName = posterShowAppName,
            showDriverName = posterShowDriverName,
            showDistance = posterShowDistance,
            showIncome = posterShowIncome,
            showRouteLine = posterShowRouteLine,
            showDuration = posterShowDuration,
            showAvgSpeed = posterShowAvgSpeed,
            showMaxSpeed = posterShowMaxSpeed
        )
    }

    var selectedDateMode by remember { mutableStateOf("LIVE") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedCustomDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedAspectRatio by remember { mutableStateOf(PosterAspectRatio.SQUARE_1_1) }
    var selectedPreset by remember { mutableStateOf(PosterPreset.NIGHT) }

    var customPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoScale by remember { mutableFloatStateOf(1f) }
    var photoOffsetX by remember { mutableFloatStateOf(0f) }
    var photoOffsetY by remember { mutableFloatStateOf(0f) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = loadRotatedBitmap(context, it)
                if (bitmap != null) {
                    customPhotoBitmap = bitmap
                    selectedPreset = PosterPreset.CUSTOM_PHOTO
                    photoScale = 1f
                    photoOffsetX = 0f
                    photoOffsetY = 0f
                } else {
                    Toast.makeText(context, "Gagal memuat foto", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val posterData = remember(
        selectedDateMode, allOrders, liveGpsPoints, todayNetIncome,
        todayOrderCount, todayTotalDistance, durationSeconds, driverName
    ) {
        if (selectedDateMode == "LIVE") {

            val avgSpd = if (durationSeconds > 0) ((todayTotalDistance / (durationSeconds / 3600.0)).toFloat()) else 0f
            val maxSpd = liveGpsPoints.maxOfOrNull { it.speed } ?: avgSpd
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())

            PosterData(
                driverName = driverName,
                distanceKm = todayTotalDistance,
                durationSeconds = durationSeconds,
                avgSpeedKmH = avgSpd,
                maxSpeedKmH = maxSpd,
                netProfit = todayNetIncome,
                orderCount = todayOrderCount,
                gpsPoints = liveGpsPoints,
                dateFormatted = dateStr
            )
        } else {

            val dayOrders = allOrders.filter { it.tanggal == selectedDateMode }
            val mergedPoints = dayOrders.flatMap { parseGpsPointsFromJson(it.trackGpsJson) }
            val totalDist = dayOrders.sumOf { it.jarakTempuh }
            val totalDur = dayOrders.sumOf { it.durasi }
            val totalProfit = dayOrders.sumOf { it.pendapatanBersih }
            val avgSpd = if (totalDur > 0) ((totalDist / (totalDur / 3600.0)).toFloat()) else 0f
            val maxSpd = mergedPoints.maxOfOrNull { it.speed } ?: avgSpd

            PosterData(
                driverName = driverName,
                distanceKm = totalDist,
                durationSeconds = totalDur,
                avgSpeedKmH = avgSpd,
                maxSpeedKmH = maxSpd,
                netProfit = totalProfit,
                orderCount = dayOrders.size,
                gpsPoints = mergedPoints,
                dateFormatted = selectedDateMode
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Poster Rute",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFF00AA13).copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    tint = Color(0xFF00AA13),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Pilih rute",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            val dateLabel = remember(selectedDateMode, allOrders) {
                if (selectedDateMode != "LIVE" && selectedDateMode.isNotBlank()) {
                    try {
                        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDateMode)
                        if (parsedDate != null) {
                            val disp = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(parsedDate)
                            val count = allOrders.count { it.tanggal == selectedDateMode }
                            if (count > 0) "$disp ($count pesanan)" else disp
                        } else {
                            selectedDateMode
                        }
                    } catch (_: Exception) {
                        selectedDateMode
                    }
                } else {
                    "Pilih tanggal"
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDateMode == "LIVE",
                        onClick = { selectedDateMode = "LIVE" },
                        label = { Text("Hari Ini", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00AA13),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedDateMode != "LIVE",
                        onClick = { showDatePickerDialog = true },
                        label = { Text(dateLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00AA13),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        if (showDatePickerDialog) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedCustomDateMillis ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedCustomDateMillis = millis
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }
                                selectedDateMode = sdf.format(Date(millis))
                            }
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("Pilih", color = Color(0xFF00AA13), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Batal")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Rasio poster",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(PosterAspectRatio.entries.toList()) { ratio ->
                    val isSelected = selectedAspectRatio == ratio
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAspectRatio = ratio },
                        label = {
                            Text(
                                text = ratio.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (ratio == PosterAspectRatio.SQUARE_1_1) Icons.Rounded.CropSquare else Icons.Rounded.CropPortrait,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00AA13),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = Color(0xFF00AA13)
                        )
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tema poster",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (selectedPreset == PosterPreset.CUSTOM_PHOTO) {
                    TextButton(
                        onClick = {
                            photoScale = 1f
                            photoOffsetX = 0f
                            photoOffsetY = 0f
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Atur ulang posisi", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PosterPreset.entries) { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (preset == PosterPreset.CUSTOM_PHOTO) {
                                    selectedPreset = preset
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    selectedPreset = preset
                                }
                            }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF00AA13) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = if (isSelected) Color(0xFF00AA13).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF00AA13) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (selectedAspectRatio == PosterAspectRatio.STORY_9_16) 9f / 16f else 1f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                RouteCanvasView(
                    gpsPoints = posterData.gpsPoints,
                    preset = selectedPreset,
                    showRouteLine = posterShowRouteLine,
                    routeLineColor = posterRouteLineColor,
                    customPhotoBitmap = customPhotoBitmap?.asImageBitmap(),
                    photoScale = photoScale,
                    photoOffsetX = photoOffsetX,
                    photoOffsetY = photoOffsetY,
                    onTransformChanged = { scale, offX, offY ->
                        photoScale = scale
                        photoOffsetX = offX
                        photoOffsetY = offY
                    },
                    modifier = Modifier.fillMaxSize()
                )

                val isLightTheme = selectedPreset == PosterPreset.LIGHT
                if (posterShowAppName || posterShowDriverName) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        if (posterShowAppName) {
                            Text(
                                text = "DRIVER TRACKER",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLightTheme) Color(0xFF00AA13) else Color(0xFF00FF66)
                            )
                        }
                        if (posterShowDriverName) {
                            Text(
                                text = "DRIVER: ${posterData.driverName.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLightTheme) Color(0xFF1A202C) else Color.White
                            )
                        }
                    }
                }

                Text(
                    text = posterData.dateFormatted,
                    fontSize = 11.sp,
                    color = if (isLightTheme) Color(0xFF718096) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )

                val hasAnyBottomStats = posterShowDistance || posterShowIncome || posterShowDuration || posterShowAvgSpeed || posterShowMaxSpeed
                if (hasAnyBottomStats) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xDD0F141C)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (posterShowDistance || posterShowIncome) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (posterShowDistance) {
                                        Column {
                                            Text(
                                                text = String.format(Locale.US, "%.1f KM", posterData.distanceKm),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF00FF66)
                                            )
                                            Text(
                                                text = "TOTAL JARAK",
                                                fontSize = 9.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    if (posterShowIncome) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = posterData.netProfit.toRupiahString(),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            val hasSubStats = posterShowDuration || posterShowAvgSpeed || posterShowMaxSpeed
                            if ((posterShowDistance || posterShowIncome) && hasSubStats) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                            }

                            if (hasSubStats) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (posterShowDuration) {
                                        val h = posterData.durationSeconds / 3600
                                        val m = (posterData.durationSeconds % 3600) / 60
                                        val durText = if (h > 0) "${h}h ${m}m" else "${m}m"

                                        Text(
                                            text = "Durasi $durText",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (posterShowAvgSpeed) {
                                        Text(
                                            text = String.format(Locale.US, "Kecepatan rata-rata %.1f km/j", posterData.avgSpeedKmH),
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (posterShowMaxSpeed) {
                                        Text(
                                            text = String.format(Locale.US, "Kecepatan maksimum %.1f km/j", posterData.maxSpeedKmH),
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = {
                    val bitmap = PosterBitmapGenerator.generateBitmap(
                        context = context,
                        aspectRatio = selectedAspectRatio,
                        preset = selectedPreset,
                        customPhotoBitmap = customPhotoBitmap,
                        photoScale = photoScale,
                        photoOffsetX = photoOffsetX,
                        photoOffsetY = photoOffsetY,
                        data = posterData,
                        visibility = elementVisibility,
                        routeLineColor = posterRouteLineColor
                    )
                    val isTrans = selectedPreset == PosterPreset.TRANSPARENT
                    val uri = saveBitmapToGallery(context, bitmap, "DriverTracker_Poster_${System.currentTimeMillis()}")
                    if (uri != null) {
                        Toast.makeText(context, if (isTrans) "Poster transparan tersimpan di galeri." else "Poster tersimpan di galeri.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Gagal menyimpan poster ke galeri", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AA13))
            ) {
                Icon(imageVector = Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedPreset == PosterPreset.TRANSPARENT) "Simpan PNG transparan" else "Simpan gambar PNG",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    exportGpxFile(context, posterData)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = posterData.gpsPoints.size >= 2
            ) {
                Icon(imageVector = Icons.Rounded.Polyline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (posterData.gpsPoints.size >= 2) "Ekspor rute GPX" else "Rute GPS belum tersedia",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    val bitmap = PosterBitmapGenerator.generateBitmap(
                        context = context,
                        aspectRatio = selectedAspectRatio,
                        preset = selectedPreset,
                        customPhotoBitmap = customPhotoBitmap,
                        photoScale = photoScale,
                        photoOffsetX = photoOffsetX,
                        photoOffsetY = photoOffsetY,
                        data = posterData,
                        visibility = elementVisibility,
                        routeLineColor = posterRouteLineColor
                    )
                    sharePosterImage(context, bitmap)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bagikan gambar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun parseGpsPointsFromJson(jsonStr: String): List<GpsPoint> {
    if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
    val list = mutableListOf<GpsPoint>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                GpsPoint(
                    lat = obj.optDouble("lat", 0.0),
                    lng = obj.optDouble("lng", 0.0),
                    time = obj.optLong("time", 0L),
                    speed = obj.optDouble("speed", 0.0).toFloat()
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DriverTracker")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        resolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    }
    return uri
}

private fun sharePosterImage(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "poster_share.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "Lihat catatan rute saya di Driver Tracker.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Poster Rute"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan poster: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun exportGpxFile(context: Context, data: PosterData) {
    try {
        val points = data.gpsPoints
        if (points.size < 2) {
            Toast.makeText(context, "Belum ada rute GPS untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"DriverTracker\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <trk>\n")
        sb.append("    <name>Driver Tracker - ").append(data.driverName).append("</name>\n")
        sb.append("    <trkseg>\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        for (p in points) {
            sb.append("      <trkpt lat=\"").append(p.lat).append("\" lon=\"").append(p.lng).append("\">\n")
            if (p.time > 0) {
                sb.append("        <time>").append(sdf.format(Date(p.time))).append("</time>\n")
            }
            if (p.speed > 0) {
                val speedMs = p.speed / 3.6
                sb.append("        <speed>").append(String.format(Locale.US, "%.2f", speedMs)).append("</speed>\n")
            }
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>")

        val gpxContent = sb.toString()

        val cachePath = File(context.cacheDir, "gpx")
        cachePath.mkdirs()
        val file = File(cachePath, "route_track.gpx")
        file.writeText(gpxContent)

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Export Rute GPX Driver Tracker")
            putExtra(Intent.EXTRA_TEXT, "File GPX rute perjalanan dari Driver Tracker.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan file rute GPX"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor GPX: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun loadRotatedBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream?.close()

        val exifStream = context.contentResolver.openInputStream(uri)
        val orientation = exifStream?.use { s ->
            val exif = android.media.ExifInterface(s)
            exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
        } ?: android.media.ExifInterface.ORIENTATION_NORMAL

        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
            else -> originalBitmap
        }
    } catch (_: Exception) {
        null
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated != bitmap) {
        bitmap.recycle()
    }
    return rotated
}
