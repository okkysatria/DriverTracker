package com.example.drivertracker.ui

import com.example.drivertracker.data.local.entity.OrderRecord
import com.example.drivertracker.ui.screens.dashboard.buildMonthlyReportText
import com.example.drivertracker.ui.screens.dashboard.formatRupiah
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardReportTest {

    @Test
    fun testFormatRupiah() {
        val formatted = formatRupiah(150000.0)
        assertTrue(formatted.contains("150.000") || formatted.contains("150"))
    }

    @Test
    fun testBuildMonthlyReportText() {
        val sampleOrders = listOf(
            OrderRecord(
                id = 1,
                tanggal = "2025-03-30",
                hari = "Minggu",
                jenisOrder = "Penumpang",
                alamatAwal = "Surabaya",
                alamatPickup = "Stasiun Gubeng",
                alamatAkhir = "Tunjungan Plaza",
                jarakKePickup = 1.2,
                jarakTempuh = 5.0,
                durasi = 900,
                pendapatanKotor = 35000.0,
                biayaBensin = 5000.0,
                pendapatanBersih = 30000.0,
                catatan = "Lancar"
            )
        )

        val reportText = buildMonthlyReportText(
            period = "Maret 2025",
            netProfit = 30000.0,
            grossIncome = 35000.0,
            fuelExpense = 5000.0,
            totalTrips = 1,
            totalDistance = 6.2,
            orders = sampleOrders
        )

        assertTrue(reportText.contains("REKAP LAPORAN KEUANGAN DRIVER TRACKER"))
        assertTrue(reportText.contains("Maret 2025"))
        assertTrue(reportText.contains("Penumpang: 1 Order"))
    }
}
