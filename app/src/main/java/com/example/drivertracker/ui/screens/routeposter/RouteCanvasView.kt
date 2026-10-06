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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.drivertracker.data.model.GpsPoint
import kotlin.math.max

@Composable
fun RouteCanvasView(
    gpsPoints: List<GpsPoint>,
    preset: PosterPreset,
    showRouteLine: Boolean = true,
    routeLineColor: String = "AUTO",
    effects: PosterEffects = PosterEffects(),
    routeTransform: PosterGroupTransform = PosterGroupTransform(),
    routeSelected: Boolean = false,
    onRouteTap: () -> Unit = {},
    onRouteDrag: (Offset) -> Unit = {},
    onRouteResize: (Offset) -> Unit = {},
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
    val currentRouteTransform by rememberUpdatedState(routeTransform)

    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }

    val routeCanvasDensity = androidx.compose.ui.platform.LocalDensity.current
    val routeHandleSizePx = with(routeCanvasDensity) { 24.dp.toPx() }
    val gestureModifier = modifier.pointerInput(preset, routeSelected, showRouteLine) {
        val inputWidth = size.width.toFloat().coerceAtLeast(1f)
        val inputHeight = size.height.toFloat().coerceAtLeast(1f)
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val isStory = inputHeight / inputWidth >= 1.4f
            val topFraction = if (isStory) 0.14f else 0.15f
            val bottomFraction = if (isStory) 0.22f else 0.24f
            val centerX = inputWidth / 2f + currentRouteTransform.offsetX * inputWidth
            val centerY = (topFraction + 1f - bottomFraction) * inputHeight / 2f + currentRouteTransform.offsetY * inputHeight
            val zoneWidth = inputWidth * 0.84f * currentRouteTransform.scale
            val zoneHeight = inputHeight * (1f - topFraction - bottomFraction) * currentRouteTransform.scale
            val zoneLeft = centerX - zoneWidth / 2f
            val zoneTop = centerY - zoneHeight / 2f
            val inRouteZone = showRouteLine && down.position.x in zoneLeft..(zoneLeft + zoneWidth) &&
                down.position.y in zoneTop..(zoneTop + zoneHeight)
            val inResizeHandle = routeSelected && inRouteZone &&
                down.position.x >= zoneLeft + zoneWidth - routeHandleSizePx * 1.5f &&
                down.position.y >= zoneTop + zoneHeight - routeHandleSizePx * 1.5f

            var previousPosition = down.position
            var accumulatedDrag = Offset.Zero
            var dragging = false
            var multiTouch = false
            var photoScaleValue = currentScale
            var photoOffsetXValue = currentOffsetX
            var photoOffsetYValue = currentOffsetY

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    multiTouch = true
                    if (preset == PosterPreset.CUSTOM_PHOTO && onTransformChanged != null) {
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (kotlin.math.abs(zoom - 1f) > 0.015f) {
                            photoScaleValue = (photoScaleValue * zoom).coerceIn(0.2f, 5.0f)
                        }
                        photoOffsetXValue += pan.x / inputWidth
                        photoOffsetYValue += pan.y / inputHeight
                        onTransformChanged(photoScaleValue, photoOffsetXValue, photoOffsetYValue)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                    continue
                }
                if (multiTouch) {
                    if (pressed.isEmpty()) break
                    continue
                }

                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) {
                    if (!multiTouch && !dragging && inRouteZone && !inResizeHandle) onRouteTap()
                    break
                }

                val delta = change.position - previousPosition
                previousPosition = change.position
                accumulatedDrag += delta
                if (!dragging && accumulatedDrag.getDistance() > viewConfiguration.touchSlop) {
                    dragging = true
                    if (routeSelected && inRouteZone) {
                        change.consume()
                        if (inResizeHandle) onRouteResize(accumulatedDrag) else onRouteDrag(accumulatedDrag)
                    }
                } else if (dragging && routeSelected && inRouteZone) {
                    change.consume()
                    if (inResizeHandle) onRouteResize(delta) else onRouteDrag(delta)
                }
            }
        }
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
            }
        } else {
            drawPresetBackground(preset, width, height)
        }

        if (showRouteLine && gpsPoints.size >= 2) {
            val isStoryCanvas = height / width >= 1.4f
            val padTopFraction = if (isStoryCanvas) 0.14f else 0.15f
            val padBottomFraction = if (isStoryCanvas) 0.22f else 0.24f
            val routeCenter = Offset(width / 2f, (padTopFraction + (1f - padBottomFraction)) * height / 2f)
            val mapped = RouteCanvasProjection.project(
                gpsPoints = gpsPoints,
                canvasWidth = width,
                canvasHeight = height,
                sidePaddingFraction = 0.08f,
                topPaddingFraction = padTopFraction,
                bottomPaddingFraction = padBottomFraction
            ).map {
                val point = Offset(it.x, it.y)
                val transformed = routeCenter + (point - routeCenter) * routeTransform.scale +
                    Offset(routeTransform.offsetX * width, routeTransform.offsetY * height)
                transformed to it.speed
            }

        val isCustomColor = routeLineColor != "AUTO" && routeLineColor.isNotBlank()
        val customColorParsed = if (isCustomColor) {
            try { Color(android.graphics.Color.parseColor(routeLineColor)) } catch (_: Exception) { Color(0xFF6FAEB8) }
        } else Color(0xFF6FAEB8)
        val effectScale = width / 360f * routeTransform.scale

        for (i in 0 until mapped.size - 1) {
            val (p1, s1) = mapped[i]
            val (p2, s2) = mapped[i + 1]
            val speed = max(s1, s2)

            val color = if (isCustomColor) {
                customColorParsed
            } else {
                when {
                    speed < 15f -> Color(0xFFD98983)
                    speed in 15f..30f -> Color(0xFFD8B968)
                    else -> Color(0xFF6FAEB8)
                }
            }

            if (effects.routeShadowEnabled) {
                val shadowColor = parseEffectColor(effects.routeShadowColor, Color.Black)
                val shadowOffset = effects.routeShadowSize * effectScale * 0.3f
                drawLine(
                    color = shadowColor.copy(alpha = 0.24f),
                    start = p1 + Offset(shadowOffset, shadowOffset),
                    end = p2 + Offset(shadowOffset, shadowOffset),
                    strokeWidth = (9f + effects.routeShadowSize * 0.5f) * effectScale,
                    cap = StrokeCap.Round
                )
            }

            if (effects.routeOutlineEnabled) {
                drawLine(
                    color = parseEffectColor(effects.routeOutlineColor, Color.White),
                    start = p1,
                    end = p2,
                    strokeWidth = (10f + effects.routeOutlineSize * 1.5f) * effectScale,
                    cap = StrokeCap.Round
                )
            }

            drawLine(
                color = color,
                start = p1,
                end = p2,
                strokeWidth = 8f * effectScale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = p1,
                end = p2,
                strokeWidth = 1.2f * effectScale,
                cap = StrokeCap.Round
            )
        }

        if (mapped.isNotEmpty()) {
            val startPt = mapped.first().first
            drawCircle(color = Color.Black.copy(alpha = 0.16f), radius = 7.5f * effectScale, center = startPt + Offset(0f, effectScale))
            drawCircle(color = Color.White.copy(alpha = 0.94f), radius = 7.2f * effectScale, center = startPt)
            drawCircle(color = Color(0xFF70B99A), radius = 5.2f * effectScale, center = startPt)
        }

        if (mapped.size > 1) {
            val endPt = mapped.last().first
            drawCircle(color = Color.Black.copy(alpha = 0.16f), radius = 7.5f * effectScale, center = endPt + Offset(0f, effectScale))
            drawCircle(color = Color.White.copy(alpha = 0.94f), radius = 7.2f * effectScale, center = endPt)
            drawCircle(color = Color(0xFFD98692), radius = 5.2f * effectScale, center = endPt)
        }
        }

        if (routeSelected && showRouteLine) {
            val topFraction = if (height / width >= 1.4f) 0.14f else 0.15f
            val bottomFraction = if (height / width >= 1.4f) 0.22f else 0.24f
            val groupCenter = Offset(
                width / 2f + routeTransform.offsetX * width,
                (topFraction + 1f - bottomFraction) * height / 2f + routeTransform.offsetY * height
            )
            val groupWidth = width * 0.84f * routeTransform.scale
            val groupHeight = height * (1f - topFraction - bottomFraction) * routeTransform.scale
            val groupTopLeft = Offset(groupCenter.x - groupWidth / 2f, groupCenter.y - groupHeight / 2f)
            drawRect(
                color = Color(0xFF00AA13),
                topLeft = groupTopLeft,
                size = Size(groupWidth, groupHeight),
                style = Stroke(width = 1.5f * width / 360f)
            )
            val handleSize = 24.dp.toPx()
            val handleTopLeft = Offset(groupTopLeft.x + groupWidth - handleSize, groupTopLeft.y + groupHeight - handleSize)
            drawRoundRect(
                color = Color(0xFF00AA13),
                topLeft = handleTopLeft,
                size = Size(handleSize, handleSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )
            drawLine(
                color = Color.White,
                start = Offset(handleTopLeft.x + handleSize * 0.3f, handleTopLeft.y + handleSize * 0.7f),
                end = Offset(handleTopLeft.x + handleSize * 0.7f, handleTopLeft.y + handleSize * 0.3f),
                strokeWidth = 1.8.dp.toPx()
            )
        }
    }
    if (showRouteLine && gpsPoints.size < 2) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val isStory = canvasHeight / canvasWidth >= 1.4f
                    val topFraction = if (isStory) 0.14f else 0.15f
                    val bottomFraction = if (isStory) 0.22f else 0.24f
                    translationX = routeTransform.offsetX * canvasWidth
                    translationY = (routeTransform.offsetY + (topFraction - bottomFraction) / 2f) * canvasHeight
                    scaleX = routeTransform.scale
                    scaleY = routeTransform.scale
                },
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

private fun parseEffectColor(value: String, fallback: Color): Color =
    try { Color(android.graphics.Color.parseColor(value)) } catch (_: IllegalArgumentException) { fallback }

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
