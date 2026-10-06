package com.example.drivertracker.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivertracker.ui.MainViewModel
import com.example.drivertracker.ui.utils.RupiahVisualTransformation
import com.example.drivertracker.ui.utils.parseRupiahToDouble
import com.example.drivertracker.ui.utils.toRupiahString
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val trackingIntervalSec by viewModel.trackingIntervalSec.collectAsStateWithLifecycle()
    val konsumsiBbm by viewModel.konsumsiBbm.collectAsStateWithLifecycle()
    val hargaBensin by viewModel.hargaBensin.collectAsStateWithLifecycle()
    val isEstimasiBensinAktif by viewModel.isEstimasiBensinAktif.collectAsStateWithLifecycle()
    val isKomisiAktif by viewModel.isKomisiAktif.collectAsStateWithLifecycle()
    val persenKomisi by viewModel.persenKomisi.collectAsStateWithLifecycle()
    val driverName by viewModel.driverName.collectAsStateWithLifecycle()

    var konsumsiInput by remember(konsumsiBbm) { mutableStateOf(konsumsiBbm.toString()) }
    var hargaInput by remember(hargaBensin) {
        mutableStateOf(if (hargaBensin > 0) hargaBensin.toLong().toString() else "")
    }
    var persenKomisiInput by remember(persenKomisi) { mutableStateOf(persenKomisi.toInt().toString()) }
    var driverNameInput by remember(driverName) { mutableStateOf(driverName) }

    var showDeleteStep1Dialog by remember { mutableStateOf(false) }
    var showDeleteStep2Dialog by remember { mutableStateOf(false) }

    val showRadarHistory by viewModel.showRadarHistory.collectAsStateWithLifecycle()
    val showRadarAi by viewModel.showRadarAi.collectAsStateWithLifecycle()
    val customOnnxModelName by viewModel.customOnnxModelName.collectAsStateWithLifecycle()

    var isImportingOnnx by remember { mutableStateOf(false) }
    var onnxImportMessage by remember { mutableStateOf("") }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val content = context.contentResolver.openInputStream(it)?.bufferedReader().use { reader ->
                        reader?.readText()
                    } ?: ""
                    if (content.isNotBlank()) {
                        val (imported, skipped) = viewModel.importJsonData(content)
                        Toast.makeText(
                            context,
                            "Impor berhasil. $imported pesanan ditambahkan, $skipped duplikat dilewati.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengimpor file backup JSON: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importOnnxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isImportingOnnx = true
                onnxImportMessage = ""
                try {
                    val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        if (nameIndex >= 0) cursor.getString(nameIndex) else "model.onnx"
                    } ?: "model.onnx"

                    val result = viewModel.importOnnxModel(context, it, fileName)
                    if (result.isSuccess) {
                        val info = result.getOrThrow()
                        onnxImportMessage = "Model berhasil diimpor.\n${info.fileName} (${info.fileSizeFormatted})"
                        Toast.makeText(context, "Model ONNX berhasil diimpor!", Toast.LENGTH_SHORT).show()
                    } else {
                        onnxImportMessage = "Gagal mengimpor model: ${result.exceptionOrNull()?.message}"
                        Toast.makeText(context, "Gagal mengimpor model: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    onnxImportMessage = "Gagal mengimpor model: ${e.message}"
                    Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isImportingOnnx = false
                }
            }
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(11.dp).size(24.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Atur tampilan, GPS, biaya, dan data aplikasi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            contentDescription = null,
                            tint = Color(0xFF00AA13)
                        )
                        Text(
                            text = "Mode Gelap",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00AA13))
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                OutlinedTextField(
                    value = driverNameInput,
                    onValueChange = {
                        driverNameInput = it
                        viewModel.setDriverName(it)
                    },
                    label = { Text("Nama pengemudi") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalGasStation,
                            contentDescription = null,
                            tint = Color(0xFF00AA13)
                        )
                        Text(
                            text = "Estimasi biaya BBM",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = isEstimasiBensinAktif,
                        onCheckedChange = { viewModel.setEstimasiBensinAktif(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00AA13))
                    )
                }

                AnimatedVisibility(visible = isEstimasiBensinAktif) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {

                        OutlinedTextField(
                            value = konsumsiInput,
                            onValueChange = { str ->
                                konsumsiInput = str
                                str.replace(',', '.').toFloatOrNull()
                                    ?.takeIf { it.isFinite() && it > 0f }
                                    ?.let { viewModel.setKonsumsiBbm(it) }
                            },
                            label = { Text("Konsumsi BBM (KM/L)") },
                            suffix = { Text("KM/L") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = hargaInput,
                            onValueChange = { str ->
                                val digits = str.filter { it.isDigit() }.take(8)
                                hargaInput = if (digits.startsWith("0") && digits.length > 1) {
                                    digits.trimStart('0')
                                } else {
                                    digits
                                }
                                viewModel.setHargaBensin(hargaInput.toDoubleOrNull() ?: 0.0)
                            },
                            visualTransformation = RupiahVisualTransformation(),
                            label = { Text("Harga Bensin Per Liter") },
                            placeholder = { Text("0") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Percent,
                            contentDescription = null,
                            tint = Color(0xFF00AA13)
                        )
                        Text(
                        text = "Komisi aplikasi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = isKomisiAktif,
                        onCheckedChange = { viewModel.setKomisiAktif(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00AA13))
                    )
                }

                AnimatedVisibility(visible = isKomisiAktif) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Komisi: ${persenKomisi.toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Slider(
                            value = persenKomisi,
                            onValueChange = { valFloat ->
                                viewModel.setPersenKomisi(valFloat)
                                persenKomisiInput = valFloat.toInt().toString()
                            },
                            valueRange = 0f..30f,
                            steps = 30,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00AA13),
                                activeTrackColor = Color(0xFF00AA13)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0f, 15f, 20f).forEach { p ->
                                FilterChip(
                                    selected = persenKomisi.toInt() == p.toInt(),
                                    onClick = {
                                        viewModel.setPersenKomisi(p)
                                        persenKomisiInput = p.toInt().toString()
                                    },
                                    label = { Text("${p.toInt()}%") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00AA13),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GpsFixed,
                        contentDescription = null,
                        tint = Color(0xFF00AA13)
                    )
                    Text(
                        text = "Interval GPS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val intervals = listOf(3, 5, 10)
                    intervals.forEach { sec ->
                        val isSelected = trackingIntervalSec == sec
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTrackingIntervalSec(sec) },
                            label = { Text("${sec} detik", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00AA13),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Backup,
                        contentDescription = null,
                        tint = Color(0xFF00AA13)
                    )
                    Text(
                        text = "Data dan cadangan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val jsonStr = viewModel.exportJsonData()
                                shareExportFile(context, jsonStr, "driver_tracker_backup.json", "application/json")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.DataObject, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val csvStr = viewModel.exportCsvData()
                                shareExportFile(context, csvStr, "driver_tracker_export.csv", "text/csv")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        importJsonLauncher.launch("application/json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Impor cadangan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Button(
                    onClick = { showDeleteStep1Dialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE2737))
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus data", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Radar,
                        contentDescription = null,
                        tint = Color(0xFF00AA13)
                    )
                    Text(
                        text = "Pengaturan radar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Grain,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Tampilkan hotspot di peta",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Berdasarkan riwayat pesanan dan lokasi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = showRadarAi,
                        onCheckedChange = { viewModel.setRadarShowAi(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00AA13))
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Tampilkan riwayat pesanan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tampilkan lokasi pesanan sebelumnya",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = showRadarHistory,
                        onCheckedChange = { viewModel.setRadarShowHistory(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00AA13))
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = Color(0xFF1E88E5)
                    )
                    Column {
                        Text(
                            text = "Model analisis (ONNX)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Impor model dari riwayat pesanan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (customOnnxModelName.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E88E5).copy(alpha = 0.10f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Model aktif",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E88E5)
                                )
                                Text(
                                    text = customOnnxModelName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                            text = "Model belum tersedia. Menggunakan metode bawaan.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (onnxImportMessage.isNotBlank()) {
                    Text(
                        text = onnxImportMessage,
                        fontSize = 12.sp,
                        color = if (onnxImportMessage.startsWith("Model berhasil")) Color(0xFF1E88E5) else MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { importOnnxLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        enabled = !isImportingOnnx
                    ) {
                        if (isImportingOnnx) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor .onnx", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (customOnnxModelName.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.removeOnnxModel(context)
                                    onnxImportMessage = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                            )
                        ) {
                            Icon(
                                Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Hapus Model",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Driver Tracker",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pencatatan perjalanan dan penghasilan",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    if (showDeleteStep1Dialog) {
        AlertDialog(
            onDismissRequest = { showDeleteStep1Dialog = false },
            title = { Text("Hapus seluruh riwayat?", fontWeight = FontWeight.Bold) },
            text = { Text("Semua catatan pesanan akan dihapus permanen. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteStep1Dialog = false
                        showDeleteStep2Dialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE2737))
                ) {
                    Text("Lanjut", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteStep1Dialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteStep2Dialog) {
        AlertDialog(
            onDismissRequest = { showDeleteStep2Dialog = false },
            title = { Text("Konfirmasi penghapusan", fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin menghapus seluruh riwayat pesanan?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllOrders()
                        showDeleteStep2Dialog = false
                        Toast.makeText(context, "Riwayat pesanan dihapus.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE2737))
                ) {
                    Text("Hapus permanen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteStep2Dialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun shareExportFile(context: Context, content: String, fileName: String, mimeType: String) {
    try {
        val cachePath = File(context.cacheDir, "exports")
        cachePath.mkdirs()
        val file = File(cachePath, fileName)
        file.writeText(content)

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup Driver Tracker - $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Simpan File Backup"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PosterSettingsContent(viewModel: MainViewModel) {
    val posterShowAppName by viewModel.posterShowAppName.collectAsStateWithLifecycle()
    val posterShowDriverName by viewModel.posterShowDriverName.collectAsStateWithLifecycle()
    val posterShowDistance by viewModel.posterShowDistance.collectAsStateWithLifecycle()
    val posterShowIncome by viewModel.posterShowIncome.collectAsStateWithLifecycle()
    val posterShowRouteLine by viewModel.posterShowRouteLine.collectAsStateWithLifecycle()
    val posterShowDuration by viewModel.posterShowDuration.collectAsStateWithLifecycle()
    val posterShowAvgSpeed by viewModel.posterShowAvgSpeed.collectAsStateWithLifecycle()
    val posterShowMaxSpeed by viewModel.posterShowMaxSpeed.collectAsStateWithLifecycle()
    val posterRouteLineColor by viewModel.posterRouteLineColor.collectAsStateWithLifecycle()
    val posterTextOutlineEnabled by viewModel.posterTextOutlineEnabled.collectAsStateWithLifecycle()
    val posterTextOutlineColor by viewModel.posterTextOutlineColor.collectAsStateWithLifecycle()
    val posterTextOutlineSize by viewModel.posterTextOutlineSize.collectAsStateWithLifecycle()
    val posterTextShadowEnabled by viewModel.posterTextShadowEnabled.collectAsStateWithLifecycle()
    val posterTextShadowColor by viewModel.posterTextShadowColor.collectAsStateWithLifecycle()
    val posterTextShadowSize by viewModel.posterTextShadowSize.collectAsStateWithLifecycle()
    val posterRouteOutlineEnabled by viewModel.posterRouteOutlineEnabled.collectAsStateWithLifecycle()
    val posterRouteOutlineColor by viewModel.posterRouteOutlineColor.collectAsStateWithLifecycle()
    val posterRouteOutlineSize by viewModel.posterRouteOutlineSize.collectAsStateWithLifecycle()
    val posterRouteShadowEnabled by viewModel.posterRouteShadowEnabled.collectAsStateWithLifecycle()
    val posterRouteShadowColor by viewModel.posterRouteShadowColor.collectAsStateWithLifecycle()
    val posterRouteShadowSize by viewModel.posterRouteShadowSize.collectAsStateWithLifecycle()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Brush,
                        contentDescription = null,
                        tint = Color(0xFF00AA13)
                    )
                    Column {
                        Text(
                            text = "Elemen poster rute",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Atur info yang ditampilkan pada poster rute",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                PosterSettingToggleRow(
                    icon = Icons.Rounded.Title,
                    title = "Nama Aplikasi",
                    subtitle = "Tampilkan 'DRIVER TRACKER' di atas poster",
                    checked = posterShowAppName,
                    onCheckedChange = { viewModel.setPosterShowAppName(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                PosterSettingToggleRow(
                    icon = Icons.Rounded.Person,
                    title = "Nama Driver",
                    subtitle = "Tampilkan label nama pengemudi",
                    checked = posterShowDriverName,
                    onCheckedChange = { viewModel.setPosterShowDriverName(it) }
                )

                PosterStrokeShadowControls(
                    title = "Gaya teks poster",
                    outlineEnabled = posterTextOutlineEnabled,
                    onOutlineEnabledChange = { viewModel.setPosterTextOutlineEnabled(it) },
                    outlineColor = posterTextOutlineColor,
                    onOutlineColorChange = { viewModel.setPosterTextOutlineColor(it) },
                    outlineSize = posterTextOutlineSize,
                    onOutlineSizeChange = { viewModel.setPosterTextOutlineSize(it) },
                    shadowEnabled = posterTextShadowEnabled,
                    onShadowEnabledChange = { viewModel.setPosterTextShadowEnabled(it) },
                    shadowColor = posterTextShadowColor,
                    onShadowColorChange = { viewModel.setPosterTextShadowColor(it) },
                    shadowSize = posterTextShadowSize,
                    onShadowSizeChange = { viewModel.setPosterTextShadowSize(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                PosterSettingToggleRow(
                    icon = Icons.Rounded.Polyline,
                    title = "Garis Rute",
                    subtitle = "Gambarkan jejak rute GPS pada kanvas",
                    checked = posterShowRouteLine,
                    onCheckedChange = { viewModel.setPosterShowRouteLine(it) }
                )

                if (posterShowRouteLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Warna garis GPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val colorOptions = listOf(
                            Pair("AUTO", "Otomatis"),
                            Pair("#22D3EE", "Biru laut"),
                            Pair("#34D399", "Mint"),
                            Pair("#818CF8", "Indigo"),
                            Pair("#C084FC", "Ungu"),
                            Pair("#FB7185", "Coral"),
                            Pair("#FBBF24", "Amber"),
                            Pair("#F8FAFC", "Putih")
                        )
                        val colorScrollState = rememberScrollState()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(colorScrollState)
                        ) {
                            colorOptions.forEach { colorOption ->
                                val colorCode = colorOption.first
                                val label = colorOption.second
                                val isSelected = posterRouteLineColor == colorCode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setPosterRouteLineColor(colorCode) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (colorCode == "AUTO") Color(0xFF22D3EE)
                                                    else Color(android.graphics.Color.parseColor(colorCode))
                                                )
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    PosterStrokeShadowControls(
                        title = "Gaya garis rute",
                        outlineEnabled = posterRouteOutlineEnabled,
                        onOutlineEnabledChange = { viewModel.setPosterRouteOutlineEnabled(it) },
                        outlineColor = posterRouteOutlineColor,
                        onOutlineColorChange = { viewModel.setPosterRouteOutlineColor(it) },
                        outlineSize = posterRouteOutlineSize,
                        onOutlineSizeChange = { viewModel.setPosterRouteOutlineSize(it) },
                        shadowEnabled = posterRouteShadowEnabled,
                        onShadowEnabledChange = { viewModel.setPosterRouteShadowEnabled(it) },
                        shadowColor = posterRouteShadowColor,
                        onShadowColorChange = { viewModel.setPosterRouteShadowColor(it) },
                        shadowSize = posterRouteShadowSize,
                        onShadowSizeChange = { viewModel.setPosterRouteShadowSize(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                PosterSettingToggleRow(
                    icon = Icons.Rounded.Straighten,
                    title = "Total Jarak",
                    subtitle = "Tampilkan total jarak tempuh (KM)",
                    checked = posterShowDistance,
                    onCheckedChange = { viewModel.setPosterShowDistance(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                PosterSettingToggleRow(
                    icon = Icons.Rounded.Payments,
                    title = "Pendapatan",
                    subtitle = "Tampilkan nominal pendapatan (angka saja tanpa label)",
                    checked = posterShowIncome,
                    onCheckedChange = { viewModel.setPosterShowIncome(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                PosterSettingToggleRow(
                    icon = Icons.Rounded.Timer,
                    title = "Durasi Waktu",
                    subtitle = "Tampilkan lama durasi perjalanan (bawaan: nonaktif)",
                    checked = posterShowDuration,
                    onCheckedChange = { viewModel.setPosterShowDuration(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                PosterSettingToggleRow(
                    icon = Icons.Rounded.Speed,
                    title = "Rata-rata Kecepatan",
                    subtitle = "Tampilkan kecepatan rata-rata km/j (awalnya nonaktif)",
                    checked = posterShowAvgSpeed,
                    onCheckedChange = { viewModel.setPosterShowAvgSpeed(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                PosterSettingToggleRow(
                    icon = Icons.Rounded.Bolt,
                    title = "Kecepatan Maksimum",
                    subtitle = "Tampilkan kecepatan maksimum km/j (awalnya nonaktif)",
                    checked = posterShowMaxSpeed,
                    onCheckedChange = { viewModel.setPosterShowMaxSpeed(it) }
                )
            }
        }
}

@Composable
private fun PosterStrokeShadowControls(
    title: String,
    outlineEnabled: Boolean,
    onOutlineEnabledChange: (Boolean) -> Unit,
    outlineColor: String,
    onOutlineColorChange: (String) -> Unit,
    outlineSize: Float,
    onOutlineSizeChange: (Float) -> Unit,
    shadowEnabled: Boolean,
    onShadowEnabledChange: (Boolean) -> Unit,
    shadowColor: String,
    onShadowColorChange: (String) -> Unit,
    shadowSize: Float,
    onShadowSizeChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            PosterSettingToggleRow(
                icon = Icons.Rounded.Brush,
                title = "Outline / stroke",
                subtitle = "Garis tepi pada elemen",
                checked = outlineEnabled,
                onCheckedChange = onOutlineEnabledChange
            )
            if (outlineEnabled) {
                PosterEffectAppearanceControls(
                    label = "Warna dan ketebalan outline",
                    color = outlineColor,
                    onColorChange = onOutlineColorChange,
                    size = outlineSize,
                    onSizeChange = onOutlineSizeChange
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            PosterSettingToggleRow(
                icon = Icons.Rounded.Layers,
                title = "Drop shadow",
                subtitle = "Bayangan di belakang elemen",
                checked = shadowEnabled,
                onCheckedChange = onShadowEnabledChange
            )
            if (shadowEnabled) {
                PosterEffectAppearanceControls(
                    label = "Warna dan ukuran shadow",
                    color = shadowColor,
                    onColorChange = onShadowColorChange,
                    size = shadowSize,
                    onSizeChange = onShadowSizeChange
                )
            }
        }
    }
}

@Composable
private fun PosterEffectAppearanceControls(
    label: String,
    color: String,
    onColorChange: (String) -> Unit,
    size: Float,
    onSizeChange: (Float) -> Unit
) {
    val colors = listOf(
        "#000000" to "Hitam",
        "#FFFFFF" to "Putih",
        "#00AA13" to "Hijau",
        "#22D3EE" to "Biru",
        "#FBBF24" to "Kuning",
        "#FF1744" to "Merah"
    )
    var sliderValue by remember(size) { mutableFloatStateOf(size) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            colors.forEach { (hex, name) ->
                val selected = color.equals(hex, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = { onColorChange(hex) },
                    label = { Text(name, fontSize = 10.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .then(
                                    if (hex == "#FFFFFF") Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    ) else Modifier
                                )
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Ketebalan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                modifier = Modifier.weight(1f),
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSizeChange(sliderValue) },
                valueRange = 0.5f..8f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00AA13), activeTrackColor = Color(0xFF00AA13))
            )
            Text(String.format(Locale.US, "%.1f", sliderValue), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PosterSettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00AA13))
        )
    }
}
