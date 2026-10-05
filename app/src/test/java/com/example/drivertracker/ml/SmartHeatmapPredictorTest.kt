package com.example.drivertracker.ml

import com.example.drivertracker.data.local.entity.OrderRecord
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmartHeatmapPredictorTest {

    private lateinit var predictor: SmartHeatmapPredictor
    private val orderHistory = listOf(
        OrderRecord(
            tanggal = "2026-10-01",
            jenisOrder = "Penumpang",
            pendapatanBersih = 18000.0,
            latitudePickup = -7.2575,
            longitudePickup = 112.7521,
            alamatPickup = "Stasiun Gubeng"
        ),
        OrderRecord(
            tanggal = "2026-10-02",
            jenisOrder = "Makanan",
            pendapatanBersih = 12000.0,
            latitudePickup = -7.2758,
            longitudePickup = 112.7831,
            alamatPickup = "Galaxy Mall"
        ),
        OrderRecord(
            tanggal = "2026-10-03",
            jenisOrder = "Paket",
            pendapatanBersih = 15000.0,
            latitudeAwal = -7.3312,
            longitudeAwal = 112.7580,
            alamatAwal = "Kawasan Rungkut Industri"
        )
    )

    @Before
    fun setUp() {
        predictor = SmartHeatmapPredictor()
    }

    @Test
    fun testPredictHotspotsReturnsMaxTop8() {
        val hotspots = predictor.predictHotspots(
            currentLat = -7.2575,
            currentLng = 112.7521,
            orders = orderHistory,
            categoryFilter = "Semua",
            sortBy = "Paling Berpotensi"
        )

        assertTrue(hotspots.isNotEmpty())
        assertTrue(hotspots.size <= 8)
        assertEquals(1, hotspots.first().rank)
    }

    @Test
    fun testPredictHotspotsCategoryFiltering() {
        val makananHotspots = predictor.predictHotspots(
            currentLat = -7.2575,
            currentLng = 112.7521,
            orders = orderHistory,
            categoryFilter = "Makanan",
            sortBy = "Paling Berpotensi"
        )

        assertTrue(makananHotspots.isNotEmpty())
        assertTrue(makananHotspots.all { it.category.equals("Makanan", ignoreCase = true) })
    }

    @Test
    fun testPredictHotspotsSortByTerdekat() {
        val hotspots = predictor.predictHotspots(
            currentLat = -7.2575,
            currentLng = 112.7521,
            orders = orderHistory,
            categoryFilter = "Semua",
            sortBy = "Terdekat"
        )

        assertTrue(hotspots.isNotEmpty())
        val distances = hotspots.map { it.distanceKm }
        val sortedDistances = distances.sorted()
        assertEquals(sortedDistances, distances)
    }

    @Test
    fun testHotspotH3IndexAndBoundary() {
        val hotspots = predictor.predictHotspots(
            currentLat = -7.2575,
            currentLng = 112.7521,
            orders = orderHistory
        )

        val first = hotspots.first()
        assertNotNull(first.h3Index)
        assertTrue(first.h3Index.isNotBlank())
        assertTrue(first.boundaryPoints.isNotEmpty())
    }
}
