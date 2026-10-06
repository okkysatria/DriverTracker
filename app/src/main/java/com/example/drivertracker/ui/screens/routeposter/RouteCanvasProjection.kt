package com.example.drivertracker.ui.screens.routeposter

import com.example.drivertracker.data.model.GpsPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

data class ProjectedRoutePoint(
    val x: Float,
    val y: Float,
    val speed: Float
)

/** Fits recorded GPS coordinates into the unobstructed poster area. */
object RouteCanvasProjection {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val MIN_ROUTE_EXTENT_METERS = 1.0
    private const val FIT_MARGIN = 0.90f

    fun project(
        gpsPoints: List<GpsPoint>,
        canvasWidth: Float,
        canvasHeight: Float,
        sidePaddingFraction: Float,
        topPaddingFraction: Float,
        bottomPaddingFraction: Float
    ): List<ProjectedRoutePoint> {
        if (gpsPoints.isEmpty() || canvasWidth <= 0f || canvasHeight <= 0f) return emptyList()

        val validPoints = gpsPoints.filter {
            it.lat.isFinite() && it.lng.isFinite() && it.lat in -90.0..90.0 && it.lng in -180.0..180.0
        }
        if (validPoints.isEmpty()) return emptyList()

        val centerLatitude = validPoints.map { it.lat }.average() * PI / 180.0
        val longitudeScale = cos(centerLatitude).coerceAtLeast(0.01)
        val metersPerRadian = EARTH_RADIUS_METERS
        val coordinates = validPoints.map { point ->
            val x = point.lng * PI / 180.0 * metersPerRadian * longitudeScale
            val y = point.lat * PI / 180.0 * metersPerRadian
            Triple(x, y, point.speed)
        }

        val minX = coordinates.minOf { it.first }
        val maxX = coordinates.maxOf { it.first }
        val minY = coordinates.minOf { it.second }
        val maxY = coordinates.maxOf { it.second }
        val actualWidth = maxX - minX
        val actualHeight = maxY - minY

        val left = canvasWidth * sidePaddingFraction
        val right = canvasWidth - left
        val top = canvasHeight * topPaddingFraction
        val bottom = canvasHeight * (1f - bottomPaddingFraction)
        val targetWidth = (right - left).coerceAtLeast(1f) * FIT_MARGIN
        val targetHeight = (bottom - top).coerceAtLeast(1f) * FIT_MARGIN
        val targetAspect = targetWidth / targetHeight

        // Give a flat or stationary track a virtual extent only on the missing axis.
        // This keeps it centered without shrinking the route because of a fake degree range.
        val effectiveWidth: Double
        val effectiveHeight: Double
        when {
            actualWidth < MIN_ROUTE_EXTENT_METERS && actualHeight < MIN_ROUTE_EXTENT_METERS -> {
                effectiveWidth = MIN_ROUTE_EXTENT_METERS * targetAspect
                effectiveHeight = MIN_ROUTE_EXTENT_METERS
            }
            actualWidth < MIN_ROUTE_EXTENT_METERS -> {
                effectiveHeight = actualHeight
                effectiveWidth = effectiveHeight * targetAspect
            }
            actualHeight < MIN_ROUTE_EXTENT_METERS -> {
                effectiveWidth = actualWidth
                effectiveHeight = effectiveWidth / targetAspect
            }
            else -> {
                effectiveWidth = actualWidth
                effectiveHeight = actualHeight
            }
        }

        val scale = min(targetWidth / effectiveWidth.toFloat(), targetHeight / effectiveHeight.toFloat())
        val centerX = (minX + maxX) / 2.0
        val centerY = (minY + maxY) / 2.0
        val canvasCenterX = (left + right) / 2f
        val canvasCenterY = (top + bottom) / 2f

        return coordinates.map { (x, y, speed) ->
            ProjectedRoutePoint(
                x = canvasCenterX + ((x - centerX) * scale).toFloat(),
                y = canvasCenterY - ((y - centerY) * scale).toFloat(),
                speed = speed
            )
        }
    }
}
