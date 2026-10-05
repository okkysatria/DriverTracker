package com.example.drivertracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.drivertracker.ui.utils.RupiahVisualTransformation
import com.example.drivertracker.ui.utils.parseRupiahToDouble
import com.example.drivertracker.ui.utils.toRupiahString
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveOrderDialog(
    jarakKePickup: Double,
    jarakKeTujuan: Double,
    totalJarak: Double,
    durasiSeconds: Long,
    konsumsiBbm: Float,
    hargaBensin: Double,
    isEstimasiBensinAktif: Boolean = true,
    isSaving: Boolean = false,
    initialJenisOrder: String = "Penumpang",
    onDismiss: () -> Unit,
    onSaveOrder: (jenisOrder: String, pendapatanKotor: Double, catatan: String, biayaBensin: Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialJenisOrder) }
    var incomeInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val estimasiBensin = remember(totalJarak, konsumsiBbm, hargaBensin, isEstimasiBensinAktif) {
        if (isEstimasiBensinAktif && konsumsiBbm > 0) {
            (totalJarak / konsumsiBbm) * hargaBensin
        } else {
            0.0
        }
    }

    val parsedIncome = remember(incomeInput) {
        incomeInput.parseRupiahToDouble()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simpan pesanan",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Jenis layanan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "Penumpang",
                        selected = selectedCategory == "Penumpang",
                        activeColor = Color(0xFF16A34A),
                        icon = Icons.Rounded.TwoWheeler,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedCategory = "Penumpang" }
                    )
                    CategoryChip(
                        label = "Makanan",
                        selected = selectedCategory == "Makanan",
                        activeColor = Color(0xFFDC2626),
                        icon = Icons.Rounded.Restaurant,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedCategory = "Makanan" }
                    )
                    CategoryChip(
                        label = "Paket",
                        selected = selectedCategory == "Paket",
                        activeColor = Color(0xFF2563EB),
                        icon = Icons.Rounded.LocalShipping,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedCategory = "Paket" }
                    )
                }

                OutlinedTextField(
                    value = incomeInput,
                    onValueChange = { newValue ->
                        val digits = newValue.filter { it.isDigit() }.take(9)
                        incomeInput = if (digits.startsWith("0") && digits.length > 1) {
                            digits.trimStart('0')
                        } else {
                            digits
                        }
                    },
                    visualTransformation = RupiahVisualTransformation(),
                    label = { Text("Pendapatan Kotor (Rp)") },
                    placeholder = { Text("0") },
                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                    trailingIcon = if (incomeInput.isNotEmpty()) {
                        {
                            IconButton(onClick = { incomeInput = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00AA13),
                        focusedLabelColor = Color(0xFF00AA13)
                    )
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ringkasan perjalanan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Jarak jemput", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.US, "%.2f km", jarakKePickup),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00AA13)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Jarak antar", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.US, "%.2f km", jarakKeTujuan),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEE2737)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total jarak", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.US, "%.2f km", totalJarak),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Durasi", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val h = durasiSeconds / 3600
                            val m = (durasiSeconds % 3600) / 60
                            val s = durasiSeconds % 60
                            val durStr = if (h > 0) {
                                String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                            } else {
                                String.format(Locale.US, "%02d:%02d", m, s)
                            }
                            Text(durStr, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFDB813),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalGasStation,
                                contentDescription = "BBM",
                                tint = Color.Black,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Biaya Bensin",
                                fontSize = 12.sp,
                                color = Color(0xFF5D4037)
                            )
                            Text(
                                text = estimasiBensin.toRupiahString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3E2723)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            onSaveOrder(selectedCategory, parsedIncome, notesInput, estimasiBensin)
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00AA13)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .height(68.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
