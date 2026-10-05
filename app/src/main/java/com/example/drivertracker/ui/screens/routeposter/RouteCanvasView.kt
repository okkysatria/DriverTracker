package com.example.drivertracker.ui.screens.routeposter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.drivertracker.data.model.GpsPoint
import kotlin.math.max
import kotlin.math.min

@Composable
fun RouteCanvasView(
    gpsPoints: List<GpsPoint>,
    preset: PosterPreset,
    showRouteLine: Boolean = true,
    routeLineColor: String = "AUTO",
    customPhotoBitmap: ImageBitmap? = null,
    photoScale: Float = 1f,
    photoOffsetX: Float = 0f,
    photoOffsetY: Float = 0f,
    onTransformChanged: ((scale: Float, offsetX: Float, offsetY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {

    val currentScale by rememberUpdatedState(photoScale)
    val currentOffsetX by rememberUpdatedState(photoOffsetX)
    val currentOffsetY by rememberUpdatedState(photoOffsetY)

    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }

    val gestureModifier = if (preset == PosterPreset.CUSTOM_PHOTO && onTransformChanged != null) {
        modifier.pointerInput(Unit) {
            awaitEachGesture {

                awaitFirstDown(requireUnconsumed = false)

                var twoFingerActive = false
                var scale = currentScale
                var normX = currentOffsetX
                var normY = currentOffsetY

                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    when {
                        pressed.size >= 2 -> {
                            twoFingerActive = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (kotlin.math.abs(zoom - 1f) > 0.015f) {
                                scale = (scale * zoom).coerceIn(0.2f, 5.0f)
                            }
                            val w = if (canvasWidth > 1f) canvasWidth else 1000f
                            val h = if (canvasHeight > 1f) canvasHeight else 1000f

                            normX += pan.x / w
                            normY += pan.y / h

                            onTransformChanged(scale, normX, normY)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                        pressed.isEmpty() -> break
                        twoFingerActive && pressed.size < 2 -> break
                        !twoFingerActive && pressed.size < 2 -> break
                    }
                }
            }
        }
    } else {
        modifier
    }

    Box(modifier = gestureModifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        canvasWidth = width
        canvasHeight = height

        if (preset == PosterPreset.CUSTOM_PHOTO && customPhotoBitmap != null) {
            val imgW = customPhotoBitmap.width.toFloat()
            val imgH = customPhotoBitmap.height.toFloat()
            val imgAspect = imgW / imgH
            val canvasAspect = width / height

            val baseScale = if (imgAspect > canvasAspect) {
                height / imgH
            } else {
                width / imgW
            }
            val totalScale = baseScale * photoScale
            val finalW = imgW * totalScale
            val finalH = imgH * totalScale

            val left = (width - finalW) / 2f + (photoOffsetX * width)
            val top = (height - finalH) / 2f + (photoOffsetY * height)

            clipRect(0f, 0f, width, height) {
                drawImage(
                    image = customPhotoBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(finalW.toInt(), finalH.toInt())
                )

                drawRect(color = Color.Black.copy(alpha = 0.50f))
            }
        } else {
            drawPresetBackground(preset, width, height)
        }

        if (showRouteLine && gpsPoints.size >= 2) {
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLng = Double.MAX_VALUE
            var maxLng = -Double.MAX_VALUE

            for (pt in gpsPoints) {
                if (pt.lat < minLat) minLat = pt.lat
                if (pt.lat > maxLat) maxLat = pt.lat
                if (pt.lng < minLng) minLng = pt.lng
                if (pt.lng > maxLng) maxLng = pt.lng
            }

            val latDiff = max(maxLat - minLat, 0.0001)
            val lngDiff = max(maxLng - minLng, 0.0001)

            val padX = width * 0.08f
            val padTop = height * 0.15f
            val padBottom = height * 0.24f

            val availableW = width - (padX * 2f)
            val availableH = height - padTop - padBottom

            val scale = min(availableW / lngDiff, availableH / latDiff)

            val trackW = (lngDiff * scale).toFloat()
            val trackH = (latDiff * scale).toFloat()

            val offsetX = padX + (availableW - trackW) / 2f
            val offsetY = padTop + (availableH - trackH) / 2f

        val mapped = gpsPoints.map { pt ->
            val x = offsetX + ((pt.lng - minLng) * scale).toFloat()
            val y = offsetY + ((maxLat - pt.lat) * scale).toFloat()
            Offset(x, y) to pt.speed
        }

        val isCustomColor = routeLineColor != "AUTO" && routeLineColor.isNotBlank()
        val customColorParsed = if (isCustomColor) {
            try { Color(android.graphics.Color.parseColor(routeLineColor)) } catch (_: Exception) { Color(0xFF00E5FF) }
        } else Color(0xFF00E5FF)

        for (i in 0 until mapped.size - 1) {
            val (p1, s1) = mapped[i]
            val (p2, s2) = mapped[i + 1]
            val speed = max(s1, s2)

            val color = if (isCustomColor) {
                customColorParsed
            } else {
                when {
                    speed < 15f -> Color(0xFFFF3D00)
                    speed in 15f..30f -> Color(0xFFFFD600)
                    else -> Color(0xFF00E5FF)
                }
            }

            drawLine(
                color = Color.Black.copy(alpha = 0.42f),
                start = p1,
                end = p2,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )

            if (isCustomColor || speed > 30f) {
                drawLine(
                    color = (if (isCustomColor) customColorParsed else Color(0xFF22D3EE)).copy(alpha = 0.24f),
                    start = p1,
                    end = p2,
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
            }

            drawLine(
                color = color,
                start = p1,
                end = p2,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        if (mapped.isNotEmpty()) {
            val startPt = mapped.first().first
            drawCircle(color = Color(0xFF00E676).copy(alpha = 0.4f), radius = 14f, center = startPt)
            drawCircle(color = Color(0xFF00E676), radius = 10f, center = startPt)
        }

        if (mapped.size > 1) {
            val endPt = mapped.last().first
            drawCircle(color = Color(0xFFFF1744).copy(alpha = 0.4f), radius = 14f, center = endPt)
            drawCircle(color = Color(0xFFFF1744), radius = 10f, center = endPt)
        }
        }
    }
    if (showRouteLine && gpsPoints.size < 2) {
        Surface(
            modifier = Modifier.align(Alignment.Center),
            color = Color.Black.copy(alpha = 0.72f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Rute GPS belum tersedia",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                color = Color.White
            )
        }
    }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPresetBackground(
    preset: PosterPreset,
    width: Float,
    height: Float
) {
    when (preset) {
        PosterPreset.NIGHT -> {
            drawRect(color = Color(0xFF0D0F12))
            drawGridLinesCompose(width, height, Color(0xFF1F242D))
        }
        PosterPreset.LIGHT -> {
            drawRect(color = Color(0xFFF8F9FA))
            drawGridLinesCompose(width, height, Color(0xFFE2E8F0))
        }
        PosterPreset.TRANSPARENT -> {

        }
        PosterPreset.CUSTOM_PHOTO -> {
            drawRect(color = Color(0xFF0D0F12))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLinesCompose(
    width: Float,
    height: Float,
    color: Color
) {
    val step = 40f
    var x = step
    while (x < width) {
        drawLine(color = color, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
        x += step
    }
    var y = step
    while (y < height) {
        drawLine(color = color, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
        y += step
    }
}
