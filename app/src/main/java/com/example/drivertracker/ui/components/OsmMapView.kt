package com.example.drivertracker.ui.components

import android.content.Context
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.drivertracker.data.model.GpsPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

val DRIVER_TRACKER_OSM_TILE_SOURCE = XYTileSource(
    "DriverTrackerOpenStreetMap",
    0,
    19,
    256,
    ".png",
    arrayOf("https://tile.openstreetmap.org/")
)

@Composable
fun OsmMapView(
    currentLocation: Location?,
    gpsPoints: List<GpsPoint>,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "DriverTracker/1.0 (+app-id:${context.packageName})"
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(17.0)
            setTileSource(DRIVER_TRACKER_OSM_TILE_SOURCE)
        }
    }
    var hasCenteredOnDriver by remember(mapView) { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(isDarkMode) {
        mapView.setTileSource(DRIVER_TRACKER_OSM_TILE_SOURCE)
        mapView.overlayManager.tilesOverlay.setColorFilter(null)
        mapView.invalidate()
    }

    LaunchedEffect(currentLocation, gpsPoints) {
        mapView.overlays.clear()

        if (gpsPoints.isNotEmpty()) {
            val polyline = Polyline(mapView).apply {
                outlinePaint.color = android.graphics.Color.parseColor("#00AA13")
                outlinePaint.strokeWidth = 10f
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                val points = gpsPoints.map { GeoPoint(it.lat, it.lng) }
                setPoints(points)
            }
            mapView.overlays.add(polyline)
        }

        currentLocation?.let { loc ->
            val geoPoint = GeoPoint(loc.latitude, loc.longitude)

            if (loc.hasAccuracy() && loc.accuracy > 0) {
                val circlePoints = Polygon.pointsAsCircle(geoPoint, loc.accuracy.toDouble())
                val circle = Polygon(mapView).apply {
                    points = circlePoints
                    fillPaint.color = android.graphics.Color.parseColor("#3300AA13")
                    outlinePaint.color = android.graphics.Color.parseColor("#8000AA13")
                    outlinePaint.strokeWidth = 2f
                }
                mapView.overlays.add(circle)
            }

            val marker = Marker(mapView).apply {
                position = geoPoint
                icon = createMapPinDrawable(context, android.graphics.Color.rgb(8, 145, 178), MapPinGlyph.DRIVER)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Posisi Driver"
            }
            mapView.overlays.add(marker)

            if (!hasCenteredOnDriver) {
                mapView.controller.animateTo(geoPoint)
                hasCenteredOnDriver = true
            }
        }

        mapView.invalidate()
    }

    Card(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                color = Color.White.copy(alpha = 0.88f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "© OpenStreetMap contributors",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    color = Color(0xFF263238),
                    fontSize = 10.sp
                )
            }
        }
    }
}
