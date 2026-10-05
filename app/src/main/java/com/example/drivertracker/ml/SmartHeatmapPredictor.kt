package com.example.drivertracker.ml

import android.content.Context
import com.example.drivertracker.data.local.entity.OrderRecord
import com.uber.h3core.H3Core
import java.util.Calendar
import kotlin.math.*

class SmartHeatmapPredictor(private val context: Context? = null) {

    private val h3: H3Core? = runCatching { H3Core.newInstance() }.getOrNull()

    fun predictHotspots(
        currentLat: Double,
        currentLng: Double,
        orders: List<OrderRecord> = emptyList(),
        categoryFilter: String = "Semua",
        sortBy: String = "Terdekat"
    ): List<HotspotInfo> {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        val validHistoryOrders = orders.filter { it.radarPickupCoordinates() != null }
        val hasGps = currentLat != 0.0 && currentLng != 0.0
        if (!hasGps && validHistoryOrders.isEmpty()) return emptyList()

        val refLat = if (hasGps) currentLat else validHistoryOrders
            .mapNotNull { it.radarPickupCoordinates()?.first }
            .average()
        val refLng = if (hasGps) currentLng else validHistoryOrders
            .mapNotNull { it.radarPickupCoordinates()?.second }
            .average()

        val candidateHotspots = if (validHistoryOrders.isNotEmpty()) {

            val resolution = 8
            val clusters = validHistoryOrders.groupBy { order ->
                val (pLat, pLng) = order.radarPickupCoordinates()!!
                if (h3 != null) {
                    try {
                        h3.latLngToCellAddress(pLat, pLng, resolution)
                    } catch (_: Exception) {
                        String.format(java.util.Locale.US, "%.3f,%.3f", pLat, pLng)
                    }
                } else {
                    String.format(java.util.Locale.US, "%.3f,%.3f", pLat, pLng)
                }
            }

            clusters.map { (h3Key, clusterOrders) ->
                val count = clusterOrders.size
                val avgLat = clusterOrders.mapNotNull { it.radarPickupCoordinates()?.first }.average()
                val avgLng = clusterOrders.mapNotNull { it.radarPickupCoordinates()?.second }.average()

                val dominantCategory = clusterOrders
                    .groupBy { it.jenisOrder }
                    .maxByOrNull { it.value.size }?.key ?: "Penumpang"

                val clusterName = clusterOrders
                    .map { if (it.alamatPickup.isNotBlank()) it.alamatPickup else it.alamatAwal }
                    .filter { it.isNotBlank() && !it.contains("Mencari lokasi") }
                    .maxByOrNull { it.length } ?: "Area Hotspot Order ($dominantCategory)"

                val dist = calculateDistanceKm(refLat, refLng, avgLat, avgLng)

                val avgNetProfit = clusterOrders.map { it.pendapatanBersih }.average()
                val historyDensityBonus = (count * 6).coerceAtMost(25)
                val profitBonus = ((avgNetProfit / 10000.0) * 3).toInt().coerceAtMost(10)

                val hourBonus = when (hour) {
                    in 7..9 -> 8
                    in 11..13 -> 10
                    in 17..20 -> 12
                    in 21..23 -> 3
                    else -> -2
                }
                val weekendBonus = if (isWeekend && dominantCategory in listOf("Makanan", "Penumpang")) 5 else 0
                val distanceFactor = ((15.0 - dist.coerceIn(0.0, 15.0)) / 2.0).toInt()

                val base = 70 + historyDensityBonus + profitBonus
                val onnxScore = if (context != null && OnnxModelManager.hasCustomModel(context)) {
                    OnnxModelManager.predictScore(
                        context = context,
                        hour = hour,
                        dayOfWeek = dayOfWeek,
                        lat = avgLat,
                        lng = avgLng,
                        distanceKm = dist,
                        category = dominantCategory,
                        isWeekend = isWeekend,
                        orderCount = count
                    )
                } else null

                val finalScore = onnxScore ?: (base + hourBonus + weekendBonus + distanceFactor).coerceIn(65, 99)
                val isFromOnnx = onnxScore != null

                var h3IndexStr = h3Key
                var boundary = emptyList<Pair<Double, Double>>()

                if (h3 != null) {
                    try {
                        if (h3IndexStr.length >= 15 && !h3IndexStr.contains(",")) {
                            val h3Boundary = h3.cellToBoundary(h3IndexStr)
                            boundary = h3Boundary.map { Pair(it.lat, it.lng) }
                        } else {
                            h3IndexStr = h3.latLngToCellAddress(avgLat, avgLng, resolution)
                            val h3Boundary = h3.cellToBoundary(h3IndexStr)
                            boundary = h3Boundary.map { Pair(it.lat, it.lng) }
                        }
                    } catch (_: Exception) {
                        boundary = generateFallbackHexagon(avgLat, avgLng, 0.005)
                    }
                }

                if (boundary.isEmpty()) {
                    boundary = generateFallbackHexagon(avgLat, avgLng, 0.005)
                }

                HotspotInfo(
                    rank = 0,
                    name = clusterName,
                    latitude = avgLat,
                    longitude = avgLng,
                    distanceKm = dist,
                    gacorScore = finalScore,
                    category = dominantCategory,
                    h3Index = h3IndexStr,
                    boundaryPoints = boundary,
                    isFromRealHistory = true,
                    orderCountInZone = count,
                    isFromOnnxModel = isFromOnnx
                )
            }
        } else {
            emptyList()
        }

        val filtered = if (categoryFilter == "Semua") {
            candidateHotspots
        } else {
            candidateHotspots.filter { it.category.equals(categoryFilter, ignoreCase = true) }
        }

        val sorted = if (sortBy == "Terdekat") {
            filtered.sortedBy { it.distanceKm }
        } else {
            filtered.sortedByDescending { it.gacorScore }
        }

        return sorted.take(8).mapIndexed { idx, item ->
            item.copy(rank = idx + 1)
        }
    }

    private fun generateFallbackHexagon(centerLat: Double, centerLng: Double, radiusDegrees: Double): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until 6) {
            val angleRad = Math.toRadians(60.0 * i)
            val lat = centerLat + radiusDegrees * cos(angleRad)
            val lng = centerLng + radiusDegrees * sin(angleRad) * 1.2
            points.add(Pair(lat, lng))
        }
        return points
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c * 10).roundToInt() / 10.0
    }
}

data class HotspotInfo(
    val rank: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val gacorScore: Int,
    val category: String,
    val h3Index: String,
    val boundaryPoints: List<Pair<Double, Double>>,
    val isFromRealHistory: Boolean = false,
    val orderCountInZone: Int = 0,
    val isFromOnnxModel: Boolean = false
)

private fun OrderRecord.radarPickupCoordinates(): Pair<Double, Double>? = when {
    latitudePickup != 0.0 && longitudePickup != 0.0 -> latitudePickup to longitudePickup
    latitudeAwal != 0.0 && longitudeAwal != 0.0 -> latitudeAwal to longitudeAwal
    else -> null
}
