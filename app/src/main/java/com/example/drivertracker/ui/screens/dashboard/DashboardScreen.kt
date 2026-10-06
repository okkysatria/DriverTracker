package com.example.drivertracker.ui.screens.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivertracker.data.local.entity.OrderRecord
import androidx.core.content.FileProvider
import com.example.drivertracker.ui.MainViewModel
import com.example.drivertracker.ui.utils.toRupiahString
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class WeekPeriodOption(
    val id: String,
    val label: String,
    val startMillis: Long,
    val endMillis: Long,
    val startDateStr: String,
    val endDateStr: String
)

fun getCalendarWeeksForMonth(year: Int, month: Int): List<WeekPeriodOption> {
    val cal = Calendar.getInstance(Locale.forLanguageTag("id-ID"))
    cal.firstDayOfWeek = Calendar.MONDAY

    cal.set(year, month, 1, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)

    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_MONTH, -1)
    }

    val weeks = mutableListOf<WeekPeriodOption>()
    var weekNum = 1
    val sdfDayMonth = SimpleDateFormat("dd MMM", Locale.forLanguageTag("id-ID"))
    val sdfDay = SimpleDateFormat("dd", Locale.forLanguageTag("id-ID"))

    val targetMonthCal = Calendar.getInstance(Locale.forLanguageTag("id-ID")).apply {
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val lastDayOfTargetMonth = targetMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val endOfTargetMonthMillis = Calendar.getInstance(Locale.forLanguageTag("id-ID")).apply {
        set(year, month, lastDayOfTargetMonth, 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    while (cal.timeInMillis <= endOfTargetMonthMillis) {
        val startCal = cal.clone() as Calendar
        val startM = startCal.timeInMillis

        val endCal = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endM = endCal.timeInMillis

        val startStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startCal.time)
        val endStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCal.time)

        val startMonth = startCal.get(Calendar.MONTH)
        val endMonth = endCal.get(Calendar.MONTH)

        val dateLabel = if (startMonth == endMonth) {
            "${sdfDay.format(startCal.time)} - ${sdfDayMonth.format(endCal.time)}"
        } else {
            "${sdfDayMonth.format(startCal.time)} - ${sdfDayMonth.format(endCal.time)}"
        }

        val label = "Minggu $weekNum ($dateLabel)"

        weeks.add(
            WeekPeriodOption(
                id = "W_${startStr}_$endStr",
                label = label,
                startMillis = startM,
                endMillis = endM,
                startDateStr = startStr,
                endDateStr = endStr
            )
        )

        cal.add(Calendar.DAY_OF_MONTH, 7)
        weekNum++
    }

    return weeks
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val driverName by viewModel.driverName.collectAsStateWithLifecycle()

    val monthlyPeriods = remember(allOrders) {
        val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
        val set = mutableSetOf<String>()
        set.add(sdfMonthYear.format(Date()))
        allOrders.forEach { order ->
            if (order.jamMulai > 0L) {
                set.add(sdfMonthYear.format(Date(order.jamMulai)))
            } else if (order.tanggal.isNotBlank()) {
                try {
                    val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(order.tanggal)
                    if (d != null) set.add(sdfMonthYear.format(d))
                } catch (_: Exception) {}
            }
        }
        set.toList()
    }

    val weeklyPeriods = remember(allOrders) {
        val calendar = Calendar.getInstance(Locale.forLanguageTag("id-ID"))
        val currentMillis = System.currentTimeMillis()
        calendar.firstDayOfWeek = Calendar.MONDAY

        val list = mutableListOf<WeekPeriodOption>()
        val sdfDisplay = SimpleDateFormat("dd MMM", Locale.forLanguageTag("id-ID"))
        val sdfFull = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))

        calendar.timeInMillis = currentMillis
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        for (w in 0..7) {
            val startM = calendar.timeInMillis
            val startStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startM))
            val startDisp = sdfDisplay.format(Date(startM))

            calendar.add(Calendar.DAY_OF_YEAR, 6)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endM = calendar.timeInMillis
            val endStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endM))
            val endDisp = sdfFull.format(Date(endM))

            val label = when (w) {
                0 -> "Minggu Ini ($startDisp - $endDisp)"
                1 -> "Minggu Lalu ($startDisp - $endDisp)"
                else -> "$startDisp - $endDisp"
            }

            list.add(
                WeekPeriodOption(
                    id = "W_$startStr",
                    label = label,
                    startMillis = startM,
                    endMillis = endM,
                    startDateStr = startStr,
                    endDateStr = endStr
                )
            )

            calendar.add(Calendar.DAY_OF_YEAR, -13)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        }
        list
    }

    val currentMonthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
    }

    var filterMode by remember { mutableStateOf("BULAN") }
    var selectedMonth by remember { mutableStateOf(currentMonthName) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var selectedDayMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDayPickerDialog by remember { mutableStateOf(false) }

    var selectedWeekOption by remember { mutableStateOf<WeekPeriodOption?>(weeklyPeriods.firstOrNull()) }
    var showWeekPickerDialog by remember { mutableStateOf(false) }

    var selectedOrderForDetail by remember { mutableStateOf<OrderRecord?>(null) }

    var chartSelectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var chartSelectedWeekIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(filterMode) {
        chartSelectedDayIndex = null
        chartSelectedWeekIndex = null
    }

    val (filteredOrders, currentPeriodName) = remember(
        allOrders, filterMode, selectedDayMillis, selectedMonth, selectedWeekOption, currentMonthName
    ) {
        when (filterMode) {
            "HARI" -> {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDayMillis))
                val label = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(selectedDayMillis))
                Pair(allOrders.filter { it.tanggal == date }, label)
            }
            "BULAN" -> {
                val monthToUse = if (selectedMonth.isNotBlank()) selectedMonth else currentMonthName
                val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
                val filtered = allOrders.filter { order ->
                    val m = if (order.jamMulai > 0L) {
                        sdfMonthYear.format(Date(order.jamMulai))
                    } else {
                        try {
                            val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(order.tanggal)
                            if (d != null) sdfMonthYear.format(d) else order.tanggal
                        } catch (_: Exception) { order.tanggal }
                    }
                    m.equals(monthToUse, ignoreCase = true)
                }
                Pair(filtered, monthToUse)
            }
            "MINGGU" -> {
                val week = selectedWeekOption ?: weeklyPeriods.firstOrNull()
                if (week != null) {
                    val filtered = allOrders.filter { order ->
                        if (order.jamMulai > 0L) {
                            order.jamMulai in week.startMillis..week.endMillis
                        } else {
                            order.tanggal in week.startDateStr..week.endDateStr
                        }
                    }
                    Pair(filtered, week.label)
                } else {
                    Pair(allOrders, "Minggu Ini")
                }
            }
            else -> Pair(allOrders, selectedMonth)
        }
    }

    val totalNetProfit = remember(filteredOrders) { filteredOrders.sumOf { it.pendapatanBersih } }
    val totalGrossIncome = remember(filteredOrders) { filteredOrders.sumOf { it.pendapatanKotor } }
    val totalFuelExpense = remember(filteredOrders) { filteredOrders.sumOf { it.biayaBensin } }
    val totalTrips = remember(filteredOrders) { filteredOrders.size }
    val totalDistance = remember(filteredOrders) { filteredOrders.sumOf { it.jarakTempuh } }

    var searchQuery by remember { mutableStateOf("") }
    var orderToDelete by remember { mutableStateOf<OrderRecord?>(null) }

    val daysOfWeek = remember { listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu") }

    val chartFilteredOrders = remember(filteredOrders, filterMode, chartSelectedDayIndex, chartSelectedWeekIndex, selectedWeekOption, currentPeriodName) {
        val sdfDay = SimpleDateFormat("EEEE", Locale.forLanguageTag("id-ID"))
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        when {
            filterMode == "MINGGU" && chartSelectedDayIndex != null -> {
                val selectedDayName = daysOfWeek.getOrNull(chartSelectedDayIndex!!)
                if (selectedDayName != null) {
                    filteredOrders.filter { order ->
                        val dayName = if (order.jamMulai > 0L) {
                            sdfDay.format(Date(order.jamMulai))
                        } else {
                            try { sdfDay.format(sdfDate.parse(order.tanggal) ?: Date()) } catch (_: Exception) { "" }
                        }
                        dayName.equals(selectedDayName, ignoreCase = true)
                    }
                } else filteredOrders
            }
            filterMode == "BULAN" && chartSelectedWeekIndex != null -> {
                val weekIdx = chartSelectedWeekIndex!!
                try {
                    val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
                    val cal = Calendar.getInstance()
                    val monthDate = try { sdfMonthYear.parse(currentPeriodName) } catch (_: Exception) { null }
                    val (targetYear, targetMonth) = if (monthDate != null) {
                        cal.time = monthDate
                        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    } else {
                        cal.time = Date()
                        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    }
                    val weeks = getCalendarWeeksForMonth(targetYear, targetMonth)
                    if (weekIdx in weeks.indices) {
                        val targetWeek = weeks[weekIdx]
                        filteredOrders.filter { order ->
                            val orderTime = if (order.jamMulai > 0L) order.jamMulai
                            else try { sdfDate.parse(order.tanggal)?.time ?: 0L } catch (_: Exception) { 0L }
                            orderTime in targetWeek.startMillis..targetWeek.endMillis
                        }
                    } else filteredOrders
                } catch (_: Exception) { filteredOrders }
            }
            else -> filteredOrders
        }
    }

    val chartSelectedSubtitle = remember(filterMode, chartSelectedDayIndex, chartSelectedWeekIndex, currentPeriodName) {
        when {
            filterMode == "MINGGU" && chartSelectedDayIndex != null -> {
                daysOfWeek.getOrNull(chartSelectedDayIndex!!)
            }
            filterMode == "BULAN" && chartSelectedWeekIndex != null -> {
                try {
                    val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
                    val cal = Calendar.getInstance()
                    val monthDate = try { sdfMonthYear.parse(currentPeriodName) } catch (_: Exception) { null }
                    val (targetYear, targetMonth) = if (monthDate != null) {
                        cal.time = monthDate
                        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    } else {
                        cal.time = Date()
                        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    }
                    val weeks = getCalendarWeeksForMonth(targetYear, targetMonth)
                    if (chartSelectedWeekIndex!! in weeks.indices) {
                        weeks[chartSelectedWeekIndex!!].label
                    } else null
                } catch (_: Exception) { null }
            }
            else -> null
        }
    }

    val displayedOrders = remember(chartFilteredOrders, searchQuery) {
        if (searchQuery.isBlank()) {
            chartFilteredOrders
        } else {
            chartFilteredOrders.filter {
                it.catatan.contains(searchQuery, ignoreCase = true) ||
                        it.alamatAwal.contains(searchQuery, ignoreCase = true) ||
                        it.alamatPickup.contains(searchQuery, ignoreCase = true) ||
                        it.alamatAkhir.contains(searchQuery, ignoreCase = true) ||
                        it.jenisOrder.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            exportSlipGajiPdf(
                                context = context,
                                driverName = driverName,
                                period = currentPeriodName,
                                netProfit = totalNetProfit,
                                grossIncome = totalGrossIncome,
                                fuelExpense = totalFuelExpense,
                                totalTrips = totalTrips,
                                totalDistance = totalDistance,
                                orders = filteredOrders
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00AA13)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ekspor laporan PDF",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val reportText = buildMonthlyReportText(
                                period = currentPeriodName,
                                netProfit = totalNetProfit,
                                grossIncome = totalGrossIncome,
                                fuelExpense = totalFuelExpense,
                                totalTrips = totalTrips,
                                totalDistance = totalDistance,
                                orders = filteredOrders
                            )
                            exportReport(context, currentPeriodName, reportText)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bagikan ringkasan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Laporan keuangan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("HARI" to "Harian", "MINGGU" to "Mingguan", "BULAN" to "Bulanan").forEach { (mode, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (filterMode == mode) Color(0xFF00AA13) else Color.Transparent)
                                    .clickable { filterMode = mode }
                                    .padding(horizontal = 13.dp, vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (filterMode == mode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (filterMode == mode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filterMode == "HARI") {
                            val dayLabel = SimpleDateFormat("EEE, d MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(selectedDayMillis))
                            FilterChip(
                                selected = true,
                                onClick = { showDayPickerDialog = true },
                                label = { Text(dayLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00AA13), selectedLabelColor = Color.White)
                            )
                        } else if (filterMode == "BULAN") {
                            val monthLabel = if (selectedMonth.isNotBlank()) selectedMonth else "Pilih bulan"
                            FilterChip(
                                selected = true,
                                onClick = { showMonthPickerDialog = true },
                                label = { Text(monthLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00AA13),
                                    selectedLabelColor = Color.White
                                ),
                                border = null
                            )
                        } else if (filterMode == "MINGGU") {
                            val weekLabel = if (selectedWeekOption != null) {
                                val rawLabel = selectedWeekOption?.label ?: ""
                                val datePart = if (rawLabel.contains("(")) rawLabel.substringAfter("(").substringBefore(")") else rawLabel
                                datePart
                            } else {
                                "Pilih minggu"
                            }
                            FilterChip(
                                selected = true,
                                onClick = { showWeekPickerDialog = true },
                                label = { Text(weekLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00AA13),
                                    selectedLabelColor = Color.White
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Pendapatan bersih",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = formatRupiah(totalNetProfit),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00AA13)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricSubCard(
                                title = "Pendapatan Kotor",
                                value = formatRupiah(totalGrossIncome),
                                icon = Icons.Rounded.AccountBalanceWallet,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            MetricSubCard(
                                title = "Bensin",
                                value = formatRupiah(totalFuelExpense),
                                icon = Icons.Rounded.LocalGasStation,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricSubCard(
                                title = "Pesanan selesai",
                                value = "$totalTrips pesanan",
                                icon = Icons.Rounded.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            MetricSubCard(
                                title = "Jarak tempuh",
                                value = String.format(Locale.US, "%.1f km", totalDistance),
                                icon = Icons.Rounded.Route,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                if (filterMode == "MINGGU") {
                    WeeklyEarningsChartCard(
                        orders = filteredOrders,
                        selectedDayIndex = chartSelectedDayIndex,
                        onDaySelected = { idx -> chartSelectedDayIndex = idx }
                    )
                } else if (filterMode == "BULAN") {
                    MonthlyWeeklyBreakdownCard(
                        orders = filteredOrders,
                        monthName = currentPeriodName,
                        selectedWeekIndex = chartSelectedWeekIndex,
                        onWeekSelected = { idx -> chartSelectedWeekIndex = idx }
                    )
                }
            }

            item {
                ServiceBreakdownCard(
                    orders = chartFilteredOrders,
                    selectedSubtitle = chartSelectedSubtitle
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Riwayat pesanan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari catatan atau alamat jemput/tujuan") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }
            }

            if (displayedOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Tidak ada order ditemukan." else "Belum ada riwayat order.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedOrders, key = { it.id }) { order ->
                    OrderItemCard(
                        order = order,
                        onClick = { selectedOrderForDetail = order },
                        onDeleteClick = { orderToDelete = order }
                    )
                }
            }
        }
    }

    selectedOrderForDetail?.let { order ->
        OrderDetailDialog(
            order = order,
            onDismiss = { selectedOrderForDetail = null },
            onDelete = {
                orderToDelete = order
            }
        )
    }

    if (showDayPickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDayMillis)
        DatePickerDialog(
            onDismissRequest = { showDayPickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDayMillis = it }
                    showDayPickerDialog = false
                }) { Text("Pilih", color = Color(0xFF00AA13), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDayPickerDialog = false }) { Text("Batal") } }
        ) { DatePicker(state = datePickerState, showModeToggle = true) }
    }

    if (showMonthPickerDialog) {
        val cal = Calendar.getInstance(Locale.forLanguageTag("id-ID"))
        MonthYearPickerDialog(
            initialMonth = cal.get(Calendar.MONTH),
            initialYear = cal.get(Calendar.YEAR),
            onDismiss = { showMonthPickerDialog = false },
            onConfirm = { monthName ->
                selectedMonth = monthName
                showMonthPickerDialog = false
            }
        )
    }

    if (showWeekPickerDialog) {
        val cal = Calendar.getInstance(Locale.forLanguageTag("id-ID"))
        WeekPickerDialog(
            initialMonth = cal.get(Calendar.MONTH),
            initialYear = cal.get(Calendar.YEAR),
            initialSelectedDateMillis = selectedWeekOption?.startMillis ?: System.currentTimeMillis(),
            onDismiss = { showWeekPickerDialog = false },
            onConfirm = { weekOpt ->
                selectedWeekOption = weekOpt
                showWeekPickerDialog = false
            }
        )
    }

    orderToDelete?.let { order ->
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("Hapus pesanan", fontWeight = FontWeight.Bold) },
            text = { Text("Catatan pesanan ini akan dihapus permanen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOrder(order.id)
                        orderToDelete = null
                        if (selectedOrderForDetail?.id == order.id) {
                            selectedOrderForDetail = null
                        }
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MetricSubCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00AA13).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00AA13),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    }
}

private fun formatCompactRupiah(amount: Double): String {
    val absoluteAmount = kotlin.math.abs(amount)
    val formatted = when {
        absoluteAmount >= 1_000_000 -> String.format(Locale.forLanguageTag("id-ID"), "%.1f jt", absoluteAmount / 1_000_000)
        absoluteAmount >= 1_000 -> String.format(Locale.forLanguageTag("id-ID"), "%.0f rb", absoluteAmount / 1_000)
        else -> formatRupiah(absoluteAmount)
    }
    return if (amount < 0) "−$formatted" else formatted
}

@Composable
private fun EarningsBar(
    label: String,
    amount: Double,
    maxAmount: Double,
    selected: Boolean,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val fraction = if (maxAmount > 0.0) {
        (amount.coerceAtLeast(0.0) / maxAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val barColor = when {
        dimmed -> MaterialTheme.colorScheme.outlineVariant
        selected -> Color(0xFF008A10)
        else -> Color(0xFF00AA13)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = if (amount == 0.0) "—" else formatCompactRupiah(amount),
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(112.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.64f)
                    .fillMaxHeight(if (fraction == 0f) 0.035f else fraction)
                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF00AA13),
            maxLines = 1
        )
    }
}

@Composable
fun WeeklyEarningsChartCard(
    orders: List<OrderRecord>,
    selectedDayIndex: Int? = null,
    onDaySelected: (Int?) -> Unit = {}
) {
    val daysOfWeek = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    val dayStats = remember(orders) {
        val earningsMap = mutableMapOf<String, Double>()
        val tripsMap = mutableMapOf<String, Int>()

        daysOfWeek.forEach { day ->
            earningsMap[day] = 0.0
            tripsMap[day] = 0
        }

        orders.forEach { order ->
            var matchedDay = order.hari
            if (matchedDay.isBlank() && order.jamMulai > 0L) {
                matchedDay = SimpleDateFormat("EEEE", Locale.forLanguageTag("id-ID")).format(Date(order.jamMulai))
            }
            val key = daysOfWeek.firstOrNull { it.equals(matchedDay, ignoreCase = true) } ?: "Senin"
            earningsMap[key] = (earningsMap[key] ?: 0.0) + order.pendapatanBersih
            tripsMap[key] = (tripsMap[key] ?: 0) + 1
        }

        daysOfWeek.map { day ->
            DayChartData(
                dayName = day,
                earnings = earningsMap[day] ?: 0.0,
                tripCount = tripsMap[day] ?: 0
            )
        }
    }

    val maxEarnings = remember(dayStats) { dayStats.maxOfOrNull { it.earnings }?.coerceAtLeast(0.0) ?: 0.0 }

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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pendapatan harian",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total ${formatCompactRupiah(dayStats.sumOf { it.earnings })}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00AA13)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .height(176.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val anySelected = selectedDayIndex != null
                dayStats.forEachIndexed { index, stat ->
                    val isSelected = index == selectedDayIndex
                    EarningsBar(
                        label = stat.dayName.take(3),
                        amount = stat.earnings,
                        maxAmount = maxEarnings,
                        selected = isSelected,
                        dimmed = anySelected && !isSelected,
                        modifier = Modifier.width(42.dp),
                        onClick = { onDaySelected(if (selectedDayIndex == index) null else index) }
                    )
                }
            }
        }
    }
}

private data class DayChartData(
    val dayName: String,
    val earnings: Double,
    val tripCount: Int
)

@Composable
fun MonthlyWeeklyBreakdownCard(
    orders: List<OrderRecord>,
    monthName: String,
    selectedWeekIndex: Int? = null,
    onWeekSelected: (Int?) -> Unit = {}
) {

    val weeklyData = remember(orders, monthName) {
        val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        val monthDate = try { sdfMonthYear.parse(monthName) } catch (_: Exception) { null }
        val (targetYear, targetMonth) = if (monthDate != null) {
            cal.time = monthDate
            Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        } else {
            cal.time = Date()
            Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }

        val weeks = getCalendarWeeksForMonth(targetYear, targetMonth)
        val weekBuckets = Array(weeks.size) { mutableListOf<OrderRecord>() }

        orders.forEach { order ->
            val orderTime = if (order.jamMulai > 0L) {
                order.jamMulai
            } else {
                try { sdfDate.parse(order.tanggal)?.time ?: 0L } catch (_: Exception) { 0L }
            }
            if (orderTime > 0L) {
                val wIdx = weeks.indexOfFirst { orderTime in it.startMillis..it.endMillis }
                if (wIdx >= 0) weekBuckets[wIdx].add(order)
            }
        }

        weeks.mapIndexed { idx, wOpt ->
            val rangeLabel = wOpt.label.substringAfter("(").substringBefore(")")
            WeekInMonthData(
                weekNumber = idx + 1,
                weekLabel = "Minggu ${idx + 1}",
                dateRangeLabel = rangeLabel,
                earnings = weekBuckets[idx].sumOf { it.pendapatanBersih },
                grossIncome = weekBuckets[idx].sumOf { it.pendapatanKotor },
                tripCount = weekBuckets[idx].size,
                totalDistance = weekBuckets[idx].sumOf { it.jarakTempuh }
            )
        }
    }

    val maxEarnings = remember(weeklyData) { weeklyData.maxOfOrNull { it.earnings }?.coerceAtLeast(0.0) ?: 0.0 }

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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pendapatan mingguan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total ${formatCompactRupiah(weeklyData.sumOf { it.earnings })} • $monthName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00AA13).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${weeklyData.size} minggu",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00AA13),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .height(176.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val anySelected = selectedWeekIndex != null
                weeklyData.forEachIndexed { index, stat ->
                    val isSelected = index == selectedWeekIndex
                    EarningsBar(
                        label = "Mg ${stat.weekNumber}",
                        amount = stat.earnings,
                        maxAmount = maxEarnings,
                        selected = isSelected,
                        dimmed = anySelected && !isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onWeekSelected(if (selectedWeekIndex == index) null else index) }
                    )
                }
            }
        }
    }
}

data class WeekInMonthData(
    val weekNumber: Int,
    val weekLabel: String,
    val dateRangeLabel: String,
    val earnings: Double,
    val grossIncome: Double,
    val tripCount: Int,
    val totalDistance: Double
)

@Composable
fun ServiceBreakdownCard(
    orders: List<OrderRecord>,
    selectedSubtitle: String? = null
) {
    val totalCount = orders.size.coerceAtLeast(1)

    val penumpangCount = remember(orders) { orders.count { it.jenisOrder.equals("Penumpang", ignoreCase = true) } }
    val makananCount = remember(orders) { orders.count { it.jenisOrder.equals("Makanan", ignoreCase = true) } }
    val paketCount = remember(orders) { orders.count { it.jenisOrder.equals("Paket", ignoreCase = true) } }

    val pPct = if (orders.isEmpty()) 0 else (penumpangCount * 100) / totalCount
    val mPct = if (orders.isEmpty()) 0 else (makananCount * 100) / totalCount
    val kPct = if (orders.isEmpty()) 0 else (paketCount * 100) / totalCount

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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Komposisi layanan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    selectedSubtitle?.let {
                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${orders.size} pesanan",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (orders.isEmpty()) {
                Text(
                    text = "Belum ada data layanan pada periode ini.",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (penumpangCount > 0) {
                        Box(Modifier.weight(penumpangCount.toFloat()).fillMaxHeight().background(Color(0xFF16A34A)))
                    }
                    if (makananCount > 0) {
                        Box(Modifier.weight(makananCount.toFloat()).fillMaxHeight().background(Color(0xFFDC2626)))
                    }
                    if (paketCount > 0) {
                        Box(Modifier.weight(paketCount.toFloat()).fillMaxHeight().background(Color(0xFF2563EB)))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServiceTypePill("Penumpang", "$penumpangCount pesanan\n$pPct%", Color(0xFF16A34A), Modifier.weight(1f))
                    ServiceTypePill("Makanan", "$makananCount pesanan\n$mPct%", Color(0xFFDC2626), Modifier.weight(1f))
                    ServiceTypePill("Paket", "$paketCount pesanan\n$kPct%", Color(0xFF2563EB), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ServiceTypePill(label: String, detail: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = detail,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderItemCard(
    order: OrderRecord,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    val badgeColor = when (order.jenisOrder.lowercase()) {
                        "makanan" -> Color(0xFFDC2626)
                        "paket" -> Color(0xFF2563EB)
                        else -> Color(0xFF16A34A)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = order.jenisOrder,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = order.tanggal.ifEmpty { "Hari ini" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatRupiah(order.pendapatanBersih),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00AA13)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus order",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val pickupStr = order.alamatPickup.ifEmpty { order.alamatAwal }
            val dropStr = order.alamatAkhir

            if (pickupStr.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.RadioButtonChecked,
                        contentDescription = null,
                        tint = Color(0xFF00AA13),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pickupStr,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (dropStr.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dropStr,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Jarak: %.1f km (menuju jemput: %.1f km)", order.jarakTempuh, order.jarakKePickup),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val durMins = order.durasi / 60
                Text(
                    text = "Durasi: $durMins menit",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    return formatter.format(amount).replace(",00", "")
}

fun buildMonthlyReportText(
    period: String,
    netProfit: Double,
    grossIncome: Double,
    fuelExpense: Double,
    totalTrips: Int,
    totalDistance: Double,
    orders: List<OrderRecord>
): String {
    val penumpangCount = orders.count { it.jenisOrder.equals("Penumpang", ignoreCase = true) }
    val makananCount = orders.count { it.jenisOrder.equals("Makanan", ignoreCase = true) }
    val paketCount = orders.count { it.jenisOrder.equals("Paket", ignoreCase = true) }

    return """
*REKAP LAPORAN KEUANGAN DRIVER TRACKER*
Periode: $period
-----------------------------------
*Pendapatan bersih*: ${formatRupiah(netProfit)}
Pendapatan kotor: ${formatRupiah(grossIncome)}
Pengeluaran bensin: ${formatRupiah(fuelExpense)}
Pesanan selesai: $totalTrips
Jarak tempuh: ${String.format(Locale.US, "%.1f", totalDistance)} km

*Rincian jenis layanan*:
- Penumpang: $penumpangCount pesanan
- Makanan: $makananCount pesanan
- Paket: $paketCount pesanan

-----------------------------------
Dibuat otomatis oleh Driver Tracker
""".trimIndent()
}

fun exportReport(context: Context, period: String, reportText: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, reportText)
        putExtra(Intent.EXTRA_SUBJECT, "Laporan Rekapitulasi Driver Tracker - $period")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Bagikan Rekap Laporan Bulanan")
    context.startActivity(shareIntent)
}

fun exportSlipGajiPdf(
    context: Context,
    driverName: String,
    period: String,
    netProfit: Double,
    grossIncome: Double,
    fuelExpense: Double,
    totalTrips: Int,
    totalDistance: Double,
    orders: List<OrderRecord>
) {
    try {
        val activeDriver = driverName.trim().ifBlank { "BELUM DIATUR" }
        val penumpangOrders = orders.filter { it.jenisOrder.equals("Penumpang", ignoreCase = true) }
        val penumpangCount = penumpangOrders.size
        val penumpangIncome = penumpangOrders.sumOf { it.pendapatanKotor }

        val makananOrders = orders.filter { it.jenisOrder.equals("Makanan", ignoreCase = true) }
        val makananCount = makananOrders.size
        val makananIncome = makananOrders.sumOf { it.pendapatanKotor }

        val paketOrders = orders.filter { it.jenisOrder.equals("Paket", ignoreCase = true) }
        val paketCount = paketOrders.size
        val paketIncome = paketOrders.sumOf { it.pendapatanKotor }

        val idLocale = Locale.forLanguageTag("id-ID")
        val printDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", idLocale).format(Date())
        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val uniqueCode = "${dateCode}-${String.format(Locale.US, "%04d", totalTrips)}"
        val refNumber = "SLIP/DT/${SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())}/${String.format(Locale.US, "%04d", totalTrips)}"

        val pPct = if (totalTrips > 0) String.format(idLocale, "%.1f%%", (penumpangCount.toDouble() / totalTrips) * 100) else "0,0%"
        val mPct = if (totalTrips > 0) String.format(idLocale, "%.1f%%", (makananCount.toDouble() / totalTrips) * 100) else "0,0%"
        val kPct = if (totalTrips > 0) String.format(idLocale, "%.1f%%", (paketCount.toDouble() / totalTrips) * 100) else "0,0%"

        val pdfDoc = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val margin = 36f
        val rightEdge = 595f - margin
        val contentWidth = rightEdge - margin

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val rectF = android.graphics.RectF()

        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.textSize = 17f
        paint.letterSpacing = 0.06f
        canvas.drawText("DRIVER TRACKER INDONESIA", 297.5f, 46f, paint)

        paint.letterSpacing = 0.02f
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        paint.textSize = 11f
    canvas.drawText("RINGKASAN PENDAPATAN & KINERJA", 297.5f, 62f, paint)

        paint.color = android.graphics.Color.rgb(100, 116, 139)
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        paint.textSize = 8f
        paint.letterSpacing = 0f
        canvas.drawText("Laporan Penghasilan dan Aktivitas Operasional Mitra Pengemudi", 297.5f, 74f, paint)

        paint.color = android.graphics.Color.rgb(0, 170, 19)
        paint.strokeWidth = 2f
        paint.style = android.graphics.Paint.Style.STROKE
        canvas.drawLine(margin, 84f, rightEdge, 84f, paint)
        paint.strokeWidth = 0.8f
        canvas.drawLine(margin, 87f, rightEdge, 87f, paint)

        rectF.set(margin, 95f, rightEdge, 155f)
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(248, 250, 252)
        canvas.drawRoundRect(rectF, 6f, 6f, paint)

        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = android.graphics.Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rectF, 6f, 6f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.textSize = 8.5f

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        canvas.drawText("Nama Mitra", margin + 12f, 111f, paint)
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        canvas.drawText(": ${activeDriver.uppercase()}", margin + 115f, 111f, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        canvas.drawText("Periode Operasional", 310f, 111f, paint)
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        canvas.drawText(": $period", 415f, 111f, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        canvas.drawText("Klasifikasi", margin + 12f, 128f, paint)
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        canvas.drawText(": Mitra Pengemudi Aktif", margin + 115f, 128f, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        canvas.drawText("Waktu Penerbitan", 310f, 128f, paint)
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        canvas.drawText(": $printDate WIB", 415f, 128f, paint)

        paint.color = android.graphics.Color.rgb(100, 116, 139)
        canvas.drawText("No. Dokumen", margin + 12f, 145f, paint)
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(51, 65, 85)
        canvas.drawText(": $refNumber", margin + 115f, 145f, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        canvas.drawText("ID Registrasi", 310f, 145f, paint)
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(51, 65, 85)
        canvas.drawText(": DT-$uniqueCode", 415f, 145f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        canvas.drawRect(margin, 169f, margin + 4f, 181f, paint)

        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        canvas.drawText("I.  RINGKASAN KINERJA OPERASIONAL", margin + 10f, 179f, paint)

        val halfW = (contentWidth - 12f) / 2f

        rectF.set(margin, 188f, margin + halfW, 244f)
        paint.color = android.graphics.Color.rgb(248, 250, 252)
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRoundRect(rectF, 6f, 6f, paint)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = android.graphics.Color.rgb(203, 213, 225)
        canvas.drawRoundRect(rectF, 6f, 6f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.textSize = 19f
        paint.color = android.graphics.Color.rgb(30, 41, 59)
        canvas.drawText("$totalTrips", margin + (halfW / 2f), 213f, paint)
        paint.textSize = 8f
        paint.color = android.graphics.Color.rgb(71, 85, 105)
        canvas.drawText("JUMLAH ORDER SELESAI", margin + (halfW / 2f), 226f, paint)
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textSize = 7f
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        canvas.drawText("Total Transaksi Terverifikasi", margin + (halfW / 2f), 236f, paint)

        val card2Left = margin + halfW + 12f
        rectF.set(card2Left, 188f, rightEdge, 244f)
        paint.color = android.graphics.Color.rgb(248, 250, 252)
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRoundRect(rectF, 6f, 6f, paint)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = android.graphics.Color.rgb(203, 213, 225)
        canvas.drawRoundRect(rectF, 6f, 6f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.textSize = 19f
        paint.color = android.graphics.Color.rgb(30, 41, 59)
        canvas.drawText("${String.format(Locale.US, "%.1f", totalDistance)} KM", card2Left + (halfW / 2f), 213f, paint)
        paint.textSize = 8f
        paint.color = android.graphics.Color.rgb(71, 85, 105)
        canvas.drawText("TOTAL JARAK TEMPUH", card2Left + (halfW / 2f), 226f, paint)
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textSize = 7f
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        canvas.drawText("Akumulasi Rute Perjalanan", card2Left + (halfW / 2f), 236f, paint)

        val tableTop = 256f
        val colVolX = 265f
        val colPctX = 370f
        val colIncX = rightEdge - 12f

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        canvas.drawRect(margin, tableTop, rightEdge, tableTop + 20f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.textSize = 8f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("Jenis Layanan", margin + 12f, tableTop + 13.5f, paint)
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("Jumlah pesanan", colVolX, tableTop + 13.5f, paint)
        canvas.drawText("Persentase Kontribusi", colPctX, tableTop + 13.5f, paint)
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("Pendapatan", colIncX, tableTop + 13.5f, paint)

        fun drawServiceRow(y: Float, label: String, count: String, pct: String, income: String, isAlt: Boolean) {
            if (isAlt) {
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(248, 250, 252)
                canvas.drawRect(margin, y, rightEdge, y + 19f, paint)
            }
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(226, 232, 240)
            paint.strokeWidth = 0.8f
            canvas.drawRect(margin, y, rightEdge, y + 19f, paint)

            paint.style = android.graphics.Paint.Style.FILL
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.color = android.graphics.Color.rgb(30, 41, 59)
            paint.textSize = 8.5f
            paint.textAlign = android.graphics.Paint.Align.LEFT
            canvas.drawText(label, margin + 12f, y + 13f, paint)

            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText(count, colVolX, y + 13f, paint)
            paint.typeface = android.graphics.Typeface.DEFAULT
            paint.color = android.graphics.Color.rgb(71, 85, 105)
            canvas.drawText(pct, colPctX, y + 13f, paint)

            paint.textAlign = android.graphics.Paint.Align.RIGHT
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.color = android.graphics.Color.rgb(30, 41, 59)
            canvas.drawText(income, colIncX, y + 13f, paint)
        }

        drawServiceRow(tableTop + 20f, "Transportasi penumpang", "$penumpangCount pesanan", pPct, formatRupiah(penumpangIncome), false)
        drawServiceRow(tableTop + 39f, "Pesan antar makanan", "$makananCount pesanan", mPct, formatRupiah(makananIncome), true)
        drawServiceRow(tableTop + 58f, "Pengiriman paket", "$paketCount pesanan", kPct, formatRupiah(paketIncome), false)

        val totalRowY = tableTop + 77f
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(240, 255, 244)
        canvas.drawRect(margin, totalRowY, rightEdge, totalRowY + 20f, paint)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = android.graphics.Color.rgb(203, 213, 225)
        canvas.drawRect(margin, totalRowY, rightEdge, totalRowY + 20f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(34, 84, 61)
        paint.textSize = 8.5f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("Total", margin + 12f, totalRowY + 13.5f, paint)

        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("$totalTrips pesanan", colVolX, totalRowY + 13.5f, paint)
        canvas.drawText("100,0%", colPctX, totalRowY + 13.5f, paint)

        paint.textAlign = android.graphics.Paint.Align.RIGHT
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        paint.textSize = 9.5f
        canvas.drawText(formatRupiah(grossIncome), colIncX, totalRowY + 13.5f, paint)

        val incomeTitleY = totalRowY + 36f
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        canvas.drawRect(margin, incomeTitleY - 10f, margin + 4f, incomeTitleY + 2f, paint)

        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("II.  REKAPITULASI PENDAPATAN", margin + 10f, incomeTitleY, paint)

        val incomeBoxTop = incomeTitleY + 10f
        rectF.set(margin, incomeBoxTop, rightEdge, incomeBoxTop + 74f)
        paint.color = android.graphics.Color.rgb(240, 255, 244)
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRoundRect(rectF, 8f, 8f, paint)

        paint.color = android.graphics.Color.rgb(0, 170, 19)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rectF, 8f, 8f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.color = android.graphics.Color.rgb(34, 84, 61)
        paint.textSize = 10f
        paint.letterSpacing = 0.04f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("TOTAL PENDAPATAN BRUTO", 297.5f, incomeBoxTop + 22f, paint)

        paint.color = android.graphics.Color.rgb(0, 170, 19)
        paint.textSize = 25f
        paint.letterSpacing = 0.02f
        canvas.drawText(formatRupiah(grossIncome), 297.5f, incomeBoxTop + 50f, paint)

        paint.color = android.graphics.Color.rgb(51, 65, 85)
        paint.textSize = 8f
        paint.letterSpacing = 0f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("Penghasilan transaksi mitra, belum dipotong biaya BBM", 297.5f, incomeBoxTop + 65f, paint)

        val authTitleY = incomeBoxTop + 94f
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        canvas.drawRect(margin, authTitleY - 10f, margin + 4f, authTitleY + 2f, paint)

        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("III.  OTENTIKASI & LEGALITAS DOKUMEN", margin + 10f, authTitleY, paint)

        val signTop = authTitleY + 12f
        val leftColX = margin + 110f
        val rightColX = rightEdge - 110f

        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        paint.textSize = 8.5f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("Mitra Pengemudi yang Bersangkutan,", leftColX, signTop + 14f, paint)

        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        paint.isUnderlineText = true
        canvas.drawText(activeDriver.uppercase(), leftColX, signTop + 62f, paint)
        paint.isUnderlineText = false

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        paint.textSize = 7.5f
        canvas.drawText("Mitra Operasional Mandiri", leftColX, signTop + 74f, paint)

        canvas.drawText("Divalidasi oleh Sistem Otomatis,", rightColX, signTop + 14f, paint)

        rectF.set(rightColX - 88f, signTop + 20f, rightColX + 88f, signTop + 48f)
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(240, 255, 244)
        canvas.drawRoundRect(rectF, 4f, 4f, paint)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        paint.strokeWidth = 1.2f
        canvas.drawRoundRect(rectF, 4f, 4f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(0, 170, 19)
        paint.textSize = 7.5f
        canvas.drawText("✓ DOKUMEN DIGITAL TERVERIFIKASI", rightColX, signTop + 33f, paint)
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(34, 84, 61)
        paint.textSize = 7f
        canvas.drawText("ID Registrasi: DT-$uniqueCode", rightColX, signTop + 43f, paint)

        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        canvas.drawText("DRIVER TRACKER SYSTEM", rightColX, signTop + 62f, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(100, 116, 139)
        paint.textSize = 7.5f
        canvas.drawText("Platform Manajemen Mitra Driver", rightColX, signTop + 74f, paint)

        pdfDoc.finishPage(page)

        val safePeriod = period.replace(" ", "_").replace("/", "-")
        val docsDir = File(context.cacheDir, "documents")
        docsDir.mkdirs()
        val pdfFile = File(docsDir, "Slip_Gaji_Driver_${safePeriod}.pdf")

        val outputStream = java.io.FileOutputStream(pdfFile)
        pdfDoc.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDoc.close()

        sharePdfFile(context, pdfFile, safePeriod, period, activeDriver)

    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Gagal membuat PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun sharePdfFile(context: Context, file: File, safePeriod: String, period: String, driverName: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Penghasilan Driver - $period")
            putExtra(Intent.EXTRA_TEXT, "Berikut laporan penghasilan Driver Tracker periode $period untuk ${driverName.uppercase()}.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Penghasilan (.pdf)"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Gagal membagikan PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun exportSlipGajiDoc(
    context: Context,
    driverName: String,
    period: String,
    netProfit: Double,
    grossIncome: Double,
    fuelExpense: Double,
    totalTrips: Int,
    totalDistance: Double,
    orders: List<OrderRecord>
) {
    try {
        val activeDriver = driverName.trim().ifBlank { "BELUM DIATUR" }
        val penumpangOrders = orders.filter { it.jenisOrder.equals("Penumpang", ignoreCase = true) }
        val penumpangCount = penumpangOrders.size
        val penumpangIncome = penumpangOrders.sumOf { it.pendapatanKotor }

        val makananOrders = orders.filter { it.jenisOrder.equals("Makanan", ignoreCase = true) }
        val makananCount = makananOrders.size
        val makananIncome = makananOrders.sumOf { it.pendapatanKotor }

        val paketOrders = orders.filter { it.jenisOrder.equals("Paket", ignoreCase = true) }
        val paketCount = paketOrders.size
        val paketIncome = paketOrders.sumOf { it.pendapatanKotor }

        val idLocale = Locale.forLanguageTag("id-ID")
        val printDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", idLocale).format(Date())
        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val uniqueCode = "${dateCode}-${String.format(Locale.US, "%04d", totalTrips)}"
        val refNumber = "SLIP/DT/${SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())}/${String.format(Locale.US, "%04d", totalTrips)}"

        val pPct = if (totalTrips > 0) String.format(idLocale, "%.1f%%", (penumpangCount.toDouble() / totalTrips) * 100) else "0,0%"
        val mPct = if (totalTrips > 0) String.format(idLocale, "%.1f%%", (makananCount.toDouble() / totalTrips) * 100) else "0,0%"
        val kPct = if (totalTrips > 0) String.format(idLocale, "%.1f%%", (paketCount.toDouble() / totalTrips) * 100) else "0,0%"

        val htmlContent = """
            <!DOCTYPE html>
            <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
            <head>
            <meta charset='utf-8'>
            <title>Laporan Penghasilan Driver - $period</title>
            <style>
                body {
                    font-family: 'Segoe UI', Calibri, Arial, sans-serif;
                    color: #1A202C;
                    margin: 30px auto;
                    max-width: 680px;
                    background-color: #ffffff;
                    line-height: 1.4;
                }
            </style>
            </head>
            <body>
                <!-- Report header -->
                <div style="text-align: center; margin-bottom: 22px;">
                    <div style="font-size: 20pt; font-weight: 800; color: #00AA13; letter-spacing: 1px; margin: 0;">DRIVER TRACKER INDONESIA</div>
                    <div style="font-size: 13pt; font-weight: bold; color: #0F172A; margin-top: 4px; text-transform: uppercase;">RINGKASAN PENDAPATAN & KINERJA</div>
                    <div style="font-size: 9pt; color: #64748B; margin-top: 2px;">Laporan Penghasilan dan Aktivitas Operasional Mitra Pengemudi</div>
                    <div style="margin-top: 10px; border-bottom: 3px double #00AA13;"></div>
                </div>

                <!-- DATA IDENTITAS MITRA -->
                <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px; background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 6px;">
                    <tr>
                        <td style="padding: 8px 12px; font-size: 9pt; color: #64748B; width: 25%;">Nama Mitra</td>
                        <td style="padding: 8px 12px; font-size: 9.5pt; font-weight: bold; color: #0F172A; width: 30%;">: ${activeDriver.uppercase()}</td>
                        <td style="padding: 8px 12px; font-size: 9pt; color: #64748B; width: 22%;">Periode Operasional</td>
                        <td style="padding: 8px 12px; font-size: 9.5pt; font-weight: bold; color: #0F172A; width: 23%;">: $period</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px 12px; font-size: 9pt; color: #64748B;">Klasifikasi</td>
                        <td style="padding: 8px 12px; font-size: 9.5pt; font-weight: bold; color: #00AA13;">: Mitra Pengemudi Aktif</td>
                        <td style="padding: 8px 12px; font-size: 9pt; color: #64748B;">Waktu Penerbitan</td>
                        <td style="padding: 8px 12px; font-size: 9pt; color: #0F172A;">: $printDate WIB</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px 12px; font-size: 9pt; color: #64748B;">No. Dokumen</td>
                        <td style="padding: 8px 12px; font-size: 9pt; font-family: monospace; color: #334155; font-weight: bold;">: $refNumber</td>
                        <td style="padding: 8px 12px; font-size: 9pt; color: #64748B;">ID Registrasi</td>
                        <td style="padding: 8px 12px; font-size: 9pt; font-family: monospace; color: #334155; font-weight: bold;">: DT-$uniqueCode</td>
                    </tr>
                </table>

                <!-- BAGIAN 1: GRID KINERJA OPERASIONAL -->
                <div style="font-size: 10pt; font-weight: bold; color: #0F172A; border-left: 4px solid #00AA13; padding-left: 8px; margin-bottom: 8px; text-transform: uppercase;">
                    I.  RINGKASAN KINERJA OPERASIONAL
                </div>
                <table style="width: 100%; border-collapse: collapse; margin-bottom: 18px;">
                    <tr>
                        <td style="width: 50%; padding: 14px 10px; border: 1px solid #CBD5E1; background-color: #F8FAFC; text-align: center; border-radius: 6px;">
                            <div style="font-size: 20pt; font-weight: 800; color: #1E293B;">$totalTrips</div>
                            <div style="font-size: 8.5pt; font-weight: bold; color: #475569; text-transform: uppercase; margin-top: 2px;">PESANAN SELESAI</div>
                            <div style="font-size: 8pt; color: #64748B;">Total Transaksi Terverifikasi</div>
                        </td>
                        <td style="width: 50%; padding: 14px 10px; border: 1px solid #CBD5E1; background-color: #F8FAFC; text-align: center; border-radius: 6px;">
                            <div style="font-size: 20pt; font-weight: 800; color: #1E293B;">${String.format(Locale.US, "%.1f", totalDistance)} KM</div>
                            <div style="font-size: 8.5pt; font-weight: bold; color: #475569; text-transform: uppercase; margin-top: 2px;">TOTAL JARAK TEMPUH</div>
                            <div style="font-size: 8pt; color: #64748B;">Akumulasi Rute Perjalanan</div>
                        </td>
                    </tr>
                </table>

                <!-- TABEL 4 KOLOM: Jenis Layanan | Jumlah Pesanan | Persentase Kontribusi | Pendapatan -->
                <table style="width: 100%; border-collapse: collapse; margin-bottom: 22px;">
                    <thead>
                        <tr style="background-color: #00AA13; color: #FFFFFF;">
                            <th style="padding: 8px 12px; font-size: 8.5pt; text-align: left; border: 1px solid #00AA13;">Jenis Layanan</th>
                            <th style="padding: 8px 12px; font-size: 8.5pt; text-align: center; border: 1px solid #00AA13;">Jumlah pesanan</th>
                            <th style="padding: 8px 12px; font-size: 8.5pt; text-align: center; border: 1px solid #00AA13;">Persentase Kontribusi</th>
                            <th style="padding: 8px 12px; font-size: 8.5pt; text-align: right; border: 1px solid #00AA13;">Pendapatan</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; font-weight: bold; color: #1E293B;">Transportasi penumpang</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: center; font-weight: bold;">$penumpangCount pesanan</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: center; color: #475569;">$pPct</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: right; font-weight: bold; color: #1E293B;">${formatRupiah(penumpangIncome)}</td>
                        </tr>
                        <tr style="background-color: #F8FAFC;">
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; font-weight: bold; color: #1E293B;">Pesan antar makanan</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: center; font-weight: bold;">$makananCount pesanan</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: center; color: #475569;">$mPct</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: right; font-weight: bold; color: #1E293B;">${formatRupiah(makananIncome)}</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; font-weight: bold; color: #1E293B;">Pengiriman paket</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: center; font-weight: bold;">$paketCount pesanan</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: center; color: #475569;">$kPct</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #E2E8F0; text-align: right; font-weight: bold; color: #1E293B;">${formatRupiah(paketIncome)}</td>
                        </tr>
                        <tr style="background-color: #F0FFF4; font-weight: bold;">
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #CBD5E1; color: #22543D;">Total</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #CBD5E1; text-align: center; color: #22543D;">$totalTrips pesanan</td>
                            <td style="padding: 8px 12px; font-size: 9pt; border: 1px solid #CBD5E1; text-align: center; color: #22543D;">100,0%</td>
                            <td style="padding: 8px 12px; font-size: 9.5pt; border: 1px solid #CBD5E1; text-align: right; color: #00AA13;">${formatRupiah(grossIncome)}</td>
                        </tr>
                    </tbody>
                </table>

                <!-- BAGIAN 2: REKAPITULASI PENDAPATAN -->
                <div style="font-size: 10pt; font-weight: bold; color: #0F172A; border-left: 4px solid #00AA13; padding-left: 8px; margin-bottom: 8px; text-transform: uppercase;">
                    II.  REKAPITULASI PENDAPATAN
                </div>
                <div style="background-color: #F0FFF4; border: 2px solid #00AA13; border-radius: 8px; padding: 20px; text-align: center; margin-bottom: 24px;">
                    <div style="font-size: 11pt; font-weight: bold; color: #22543D; text-transform: uppercase; letter-spacing: 1px;">
                        TOTAL PENDAPATAN BRUTO
                    </div>
                    <div style="font-size: 28pt; font-weight: 900; color: #00AA13; margin: 8px 0; letter-spacing: 0.5px;">
                        ${formatRupiah(grossIncome)}
                    </div>
                    <div style="font-size: 9.5pt; color: #334155; font-weight: 500;">
                        Penghasilan transaksi mitra, belum dipotong biaya BBM
                    </div>
                </div>

                <!-- BAGIAN 3: OTENTIKASI & LEGALITAS DOKUMEN -->
                <div style="font-size: 10pt; font-weight: bold; color: #0F172A; border-left: 4px solid #00AA13; padding-left: 8px; margin-bottom: 8px; text-transform: uppercase;">
                    III.  OTENTIKASI & LEGALITAS DOKUMEN
                </div>
                <table style="width: 100%; border-collapse: collapse; margin-top: 14px; page-break-inside: avoid;">
                    <tr>
                        <td style="width: 50%; text-align: center; font-size: 9pt; vertical-align: top;">
                            <div style="color: #64748B;">Mitra Pengemudi yang Bersangkutan,</div>
                            <div style="height: 48px;"></div>
                            <div style="font-weight: bold; font-size: 10pt; color: #0F172A; text-decoration: underline;">${activeDriver.uppercase()}</div>
                            <div style="font-size: 8pt; color: #64748B; margin-top: 2px;">Mitra Operasional Mandiri</div>
                        </td>
                        <td style="width: 50%; text-align: center; font-size: 9pt; vertical-align: top;">
                            <div style="color: #64748B;">Divalidasi oleh Sistem Otomatis,</div>
                            <div style="height: 6px;"></div>
                            <div style="display: inline-block; border: 2px dashed #00AA13; background: #F0FFF4; color: #00AA13; padding: 6px 16px; border-radius: 6px; font-weight: bold; font-size: 8.5pt;">
                                ✓ DOKUMEN DIGITAL TERVERIFIKASI<br>
                                <span style="font-size: 7.5pt; font-weight: normal; color: #22543D;">ID Registrasi: DT-$uniqueCode</span>
                            </div>
                            <div style="height: 8px;"></div>
                            <div style="font-weight: bold; font-size: 10pt; color: #0F172A;">DRIVER TRACKER SYSTEM</div>
                            <div style="font-size: 8pt; color: #64748B; margin-top: 2px;">Platform Manajemen Mitra Driver</div>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val safePeriod = period.replace(" ", "_").replace("/", "-")
        val docsDir = File(context.cacheDir, "documents")
        docsDir.mkdirs()
        val docFile = File(docsDir, "Slip_Gaji_Driver_${safePeriod}.doc")
        docFile.writeText(htmlContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", docFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/msword"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Penghasilan Driver - $period")
            putExtra(Intent.EXTRA_TEXT, "Berikut laporan penghasilan Driver Tracker periode $period untuk ${activeDriver.uppercase()}.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Penghasilan (.doc)"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Gagal mengekspor dokumen: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MonthYearPickerDialog(
    initialMonth: Int,
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (monthName: String) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val monthShort = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedYear-- }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Tahun Lalu")
                }
                Text(
                    text = "$selectedYear",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { selectedYear++ }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Tahun Depan")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pilih Bulan:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                for (row in 0..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..2) {
                            val monthIdx = row * 3 + col
                            val isSelected = selectedMonth == monthIdx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) Color(0xFF00AA13)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { selectedMonth = monthIdx }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthShort[monthIdx],
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = "${monthNames[selectedMonth]} $selectedYear"
                    onConfirm(formatted)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AA13)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Pilih", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekPickerDialog(
    initialMonth: Int,
    initialYear: Int,
    initialSelectedDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (weekOption: WeekPeriodOption) -> Unit
) {
    val initialDateMillis = remember(initialYear, initialMonth) {
        Calendar.getInstance().apply {
            set(initialYear, initialMonth, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val pickerInitialDateMillis = remember(initialDateMillis, initialSelectedDateMillis) {
        val localDate = Calendar.getInstance().apply {
            timeInMillis = initialSelectedDateMillis ?: initialDateMillis
        }
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(localDate.get(Calendar.YEAR), localDate.get(Calendar.MONTH), localDate.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = pickerInitialDateMillis)
    val selectedWeek = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { selectedMillis ->
            val start = Calendar.getInstance().apply {
                timeInMillis = selectedMillis
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_MONTH, -1)
            }
            val end = (start.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))
            val startDate = dateFormat.format(start.time)
            val endDate = dateFormat.format(end.time)
            WeekPeriodOption(
                id = "W_${startDate}_$endDate",
                label = "Minggu (${displayFormat.format(start.time)} – ${displayFormat.format(end.time)})",
                startMillis = start.timeInMillis,
                endMillis = end.timeInMillis,
                startDateStr = startDate,
                endDateStr = endDate
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Minggu", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ketuk tanggal apa saja untuk memilih minggu Senin–Minggu.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    title = null,
                    headline = null
                )
                selectedWeek?.let { week ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF00AA13).copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color(0xFF00AA13).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.DateRange, contentDescription = null, tint = Color(0xFF00AA13))
                            Column {
                                Text("Rentang minggu terpilih", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(week.label.removePrefix("Minggu "), fontWeight = FontWeight.Bold, color = Color(0xFF00AA13))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedWeek?.let { onConfirm(it) }
                },
                enabled = selectedWeek != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AA13)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Pilih", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun OrderDetailDialog(
    order: OrderRecord,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val startTime = if (order.jamMulai > 0L) timeFormatter.format(Date(order.jamMulai)) else "-"
    val pickupTime = if (order.jamPickup > 0L) timeFormatter.format(Date(order.jamPickup)) else "-"
    val finishTime = if (order.jamSelesai > 0L) timeFormatter.format(Date(order.jamSelesai)) else "-"

    val badgeColor = when (order.jenisOrder.lowercase()) {
        "makanan" -> Color(0xFFDC2626)
        "paket" -> Color(0xFF2563EB)
        else -> Color(0xFF16A34A)
    }

    val totalDurMins = order.durasi / 60
    val totalDurSecs = order.durasi % 60
    val durationText = if (totalDurMins > 0) "$totalDurMins mnt $totalDurSecs dtk" else "$totalDurSecs dtk"
    val avgSpeed = if (order.durasi > 0) (order.jarakTempuh / (order.durasi / 3600.0)) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = order.jenisOrder,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = if (order.hari.isNotBlank()) "${order.hari}, ${order.tanggal}" else order.tanggal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF00AA13).copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF00AA13).copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ringkasan Finansial",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Pendapatan Bersih", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = formatRupiah(order.pendapatanBersih),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00AA13)
                            )
                        }
                        if (order.pendapatanKotor > 0 && order.pendapatanKotor != order.pendapatanBersih) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Pendapatan Kotor", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = formatRupiah(order.pendapatanKotor), fontSize = 12.sp)
                            }
                        }
                        if (order.biayaBensin > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Potongan BBM", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "- ${formatRupiah(order.biayaBensin)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Kinerja & Telemetri",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total jarak", fontSize = 12.sp)
                            Text(text = String.format(Locale.US, "%.2f km", order.jarakTempuh), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Jarak menuju jemput", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format(Locale.US, "%.2f km", order.jarakKePickup), fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Jarak pengantaran", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format(Locale.US, "%.2f km", order.jarakKeTujuan), fontSize = 12.sp)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total durasi", fontSize = 12.sp)
                            Text(text = durationText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Kecepatan rata-rata", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format(Locale.US, "%.1f km/jam", avgSpeed), fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Jam operasional", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$startTime  ➔  $finishTime", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Rute perjalanan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val pickupAddr = order.alamatPickup.ifEmpty { order.alamatAwal }
                        if (pickupAddr.isNotBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.RadioButtonChecked,
                                    contentDescription = null,
                                    tint = Color(0xFF00AA13),
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Titik jemput", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = pickupAddr, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        if (order.alamatAkhir.isNotBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Titik tujuan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = order.alamatAkhir, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                if (order.catatan.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Catatan tambahan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = order.catatan,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AA13)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hapus", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
