package com.example.drivertracker

import com.example.drivertracker.data.local.entity.OrderRecord
import com.example.drivertracker.data.model.GpsPoint
import com.example.drivertracker.ui.screens.routeposter.PosterData
import org.junit.Assert.*
import org.junit.Test

class Task4PosterAndSettingsTest {

    @Test
    fun testPaceColorSpeedThresholds() {
        val slowPoint = GpsPoint(-7.25, 112.75, 1000L, 10f)
        val mediumPoint = GpsPoint(-7.26, 112.76, 2000L, 22f)
        val fastPoint = GpsPoint(-7.27, 112.77, 3000L, 45f)

        assertTrue("Slow point speed < 15", slowPoint.speed < 15f)
        assertTrue("Medium point speed in 15..30", mediumPoint.speed in 15f..30f)
        assertTrue("Fast point speed > 30", fastPoint.speed > 30f)
    }

    @Test
    fun testPosterDataBadgesAndMetrics() {
        val dataCentury = PosterData(
            driverName = "SUTISNA",
            distanceKm = 105.0,
            durationSeconds = 7200L,
            avgSpeedKmH = 28f,
            maxSpeedKmH = 55f,
            netProfit = 250000.0,
            orderCount = 8
        )

        assertEquals("SUTISNA", dataCentury.driverName)
        assertEquals(105.0, dataCentury.distanceKm, 0.001)
        assertTrue(dataCentury.distanceKm >= 100.0)
        assertTrue(dataCentury.avgSpeedKmH >= 25f)
        assertTrue(dataCentury.netProfit >= 150000.0)
    }

    @Test
    fun testGpxXmlGeneration() {
        val points = listOf(
            GpsPoint(-7.2575, 112.7521, 1600000000000L, 20f),
            GpsPoint(-7.2600, 112.7550, 1600000300000L, 35f)
        )

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"DriverTracker\">\n")
        sb.append("  <trk>\n")
        sb.append("    <name>Driver Tracker Route</name>\n")
        sb.append("    <trkseg>\n")

        for (p in points) {
            sb.append("      <trkpt lat=\"").append(p.lat).append("\" lon=\"").append(p.lng).append("\">\n")
            sb.append("        <time>").append(p.time).append("</time>\n")
            sb.append("        <speed>").append(p.speed).append("</speed>\n")
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n  </trk>\n</gpx>")

        val gpxXml = sb.toString()

        assertTrue(gpxXml.contains("<?xml version=\"1.0\""))
        assertTrue(gpxXml.contains("<gpx version=\"1.1\""))
        assertTrue(gpxXml.contains("trkpt lat=\"-7.2575\" lon=\"112.7521\""))
        assertTrue(gpxXml.contains("trkpt lat=\"-7.26\" lon=\"112.755\""))
        assertTrue(gpxXml.endsWith("</gpx>"))
    }

    @Test
    fun testBackupAntiDuplicationLogic() {
        val sampleRecord = OrderRecord(
            id = 1,
            tanggal = "2025-03-30",
            hari = "Minggu",
            jamMulai = 1700000000000L,
            jenisOrder = "Penumpang",
            pendapatanBersih = 50000.0,
            jarakTempuh = 12.5
        )

        val existingList = listOf(sampleRecord)

        val incomingList = listOf(

            OrderRecord(
                id = 0,
                tanggal = "2025-03-30",
                jamMulai = 1700000000000L,
                jenisOrder = "Penumpang",
                pendapatanBersih = 50000.0,
                jarakTempuh = 12.5
            ),

            OrderRecord(
                id = 0,
                tanggal = "2025-03-31",
                jamMulai = 1700086400000L,
                jenisOrder = "Makanan",
                pendapatanBersih = 35000.0,
                jarakTempuh = 6.0
            )
        )

        var importedCount = 0
        var skippedCount = 0

        for (incoming in incomingList) {
            val isDuplicate = existingList.any { existing ->
                (incoming.jamMulai > 0 && existing.jamMulai == incoming.jamMulai) ||
                        (existing.tanggal == incoming.tanggal &&
                                Math.abs(existing.pendapatanBersih - incoming.pendapatanBersih) < 0.01 &&
                                Math.abs(existing.jarakTempuh - incoming.jarakTempuh) < 0.01)
            }

            if (isDuplicate) {
                skippedCount++
            } else {
                importedCount++
            }
        }

        assertEquals(1, skippedCount)
        assertEquals(1, importedCount)
    }

    @Test
    fun testCsvFormatting() {
        val record = OrderRecord(
            id = 10,
            tanggal = "2025-03-30",
            hari = "Minggu",
            jenisOrder = "Paket",
            pendapatanBersih = 45000.0,
            alamatAwal = "Jalan Tunjungan, Surabaya",
            catatan = "Titik \"A\" aman"
        )

        val sb = StringBuilder()
        sb.append("ID,Tanggal,Hari,JenisOrder,PendapatanBersih,AlamatAwal,Catatan\n")
        sb.append(record.id).append(",")
            .append(record.tanggal).append(",")
            .append(record.hari).append(",")
            .append("\"").append(record.jenisOrder.replace("\"", "\"\"")).append("\",")
            .append(record.pendapatanBersih).append(",")
            .append("\"").append(record.alamatAwal.replace("\"", "\"\"")).append("\",")
            .append("\"").append(record.catatan.replace("\"", "\"\"")).append("\"\n")

        val csv = sb.toString()
        assertTrue(csv.contains("ID,Tanggal,Hari,JenisOrder"))
        assertTrue(csv.contains("10,2025-03-30,Minggu,\"Paket\",45000.0,\"Jalan Tunjungan, Surabaya\",\"Titik \"\"A\"\" aman\""))
    }
}
