package com.example.drivertracker.ui.screens.routeposter

import android.content.Context
import android.graphics.*
import com.example.drivertracker.data.model.GpsPoint
import com.example.drivertracker.ui.utils.toRupiahString
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

enum class PosterAspectRatio(val width: Int, val height: Int, val label: String) {
    SQUARE_1_1(1080, 1080, "1:1 Persegi"),
    STORY_9_16(1080, 1920, "9:16 Cerita")
}

enum class PosterPreset(val title: String, val description: String) {
    NIGHT("Gelap", "Tampilan gelap minimalis"),
    LIGHT("Terang", "Tampilan terang sederhana"),
    TRANSPARENT("Transparan", "Latar transparan (PNG)"),
    CUSTOM_PHOTO("Foto kustom", "Gunakan dan atur foto latar")
}

data class PosterElementVisibility(
    val showAppName: Boolean = true,
    val showDriverName: Boolean = true,
    val showDistance: Boolean = true,
    val showIncome: Boolean = true,
    val showRouteLine: Boolean = true,
    val showDuration: Boolean = false,
    val showAvgSpeed: Boolean = false,
    val showMaxSpeed: Boolean = false
)

data class PosterEffects(
    val textOutlineEnabled: Boolean = false,
    val textOutlineColor: String = "#000000",
    val textOutlineSize: Float = 1.5f,
    val textShadowEnabled: Boolean = true,
    val textShadowColor: String = "#000000",
    val textShadowSize: Float = 2f,
    val routeOutlineEnabled: Boolean = false,
    val routeOutlineColor: String = "#FFFFFF",
    val routeOutlineSize: Float = 2f,
    val routeShadowEnabled: Boolean = true,
    val routeShadowColor: String = "#000000",
    val routeShadowSize: Float = 2f
)

data class PosterGroupTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f
)

data class PosterDraftState(
    val selectedDateMode: String = "LIVE",
    val selectedCustomDateMillis: Long? = null,
    val selectedAspectRatio: PosterAspectRatio = PosterAspectRatio.SQUARE_1_1,
    val selectedPreset: PosterPreset = PosterPreset.NIGHT,
    val customPhotoBitmap: Bitmap? = null,
    val photoScale: Float = 1f,
    val photoOffsetX: Float = 0f,
    val photoOffsetY: Float = 0f,
    val headerTransform: PosterGroupTransform = PosterGroupTransform(),
    val routeTransform: PosterGroupTransform = PosterGroupTransform(),
    val statsTransform: PosterGroupTransform = PosterGroupTransform()
)

data class PosterData(
    val driverName: String = "",
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val avgSpeedKmH: Float = 0f,
    val maxSpeedKmH: Float = 0f,
    val netProfit: Double = 0.0,
    val orderCount: Int = 0,
    val gpsPoints: List<GpsPoint> = emptyList(),
    val dateFormatted: String = ""
)

object PosterBitmapGenerator {

    fun generateBitmap(
        context: Context,
        aspectRatio: PosterAspectRatio,
        preset: PosterPreset,
        customPhotoBitmap: Bitmap? = null,
        photoScale: Float = 1f,
        photoOffsetX: Float = 0f,
        photoOffsetY: Float = 0f,
        data: PosterData,
        visibility: PosterElementVisibility = PosterElementVisibility(),
        routeLineColor: String = "AUTO",
        effects: PosterEffects = PosterEffects(),
        headerTransform: PosterGroupTransform = PosterGroupTransform(),
        routeTransform: PosterGroupTransform = PosterGroupTransform(),
        statsTransform: PosterGroupTransform = PosterGroupTransform()
    ): Bitmap {
        val width = aspectRatio.width
        val height = aspectRatio.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas, width, height, preset, customPhotoBitmap, photoScale, photoOffsetX, photoOffsetY)

        val outputScale = width / 360f
        val logicalWidth = 360f
        val logicalHeight = height / outputScale
        canvas.save()
        canvas.scale(outputScale, outputScale)

        drawHeader(canvas, logicalWidth, logicalHeight, data, preset, visibility, effects, headerTransform)

        val isStory = aspectRatio == PosterAspectRatio.STORY_9_16
        val padX = logicalWidth * 0.08f
        val padTop = if (isStory) logicalHeight * 0.14f else logicalHeight * 0.15f
        val padBottom = if (isStory) logicalHeight * 0.22f else logicalHeight * 0.24f

        if (visibility.showRouteLine) {
            drawRouteTrack(
                canvas = canvas,
                left = padX,
                top = padTop,
                right = logicalWidth - padX,
                bottom = logicalHeight - padBottom,
                canvasWidth = logicalWidth,
                canvasHeight = logicalHeight,
                gpsPoints = data.gpsPoints,
                routeLineColor = routeLineColor,
                effects = effects,
                routeTransform = routeTransform,
                emptyStateColor = Color.WHITE
            )
        }

        val hasTopStats = visibility.showDistance || visibility.showIncome
        val hasSubStats = visibility.showDuration || visibility.showAvgSpeed || visibility.showMaxSpeed
        val cardHeight = when {
            hasTopStats && hasSubStats -> 86f
            hasTopStats -> 59f
            hasSubStats -> 38f
            else -> 0f
        }
        val cardMargin = 12f
        val statsTop = logicalHeight - cardHeight - cardMargin

        drawTelemetryCard(
            canvas = canvas,
            left = 12f,
            top = statsTop,
            right = logicalWidth - 12f,
            bottom = statsTop + cardHeight,
            data = data,
            visibility = visibility,
            transform = statsTransform,
            canvasWidth = logicalWidth,
            canvasHeight = logicalHeight
        )

        canvas.restore()
        return bitmap
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        preset: PosterPreset,
        customPhotoBitmap: Bitmap?,
        photoScale: Float,
        photoOffsetX: Float,
        photoOffsetY: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (preset == PosterPreset.CUSTOM_PHOTO && customPhotoBitmap != null) {
            val srcWidth = customPhotoBitmap.width.toFloat()
            val srcHeight = customPhotoBitmap.height.toFloat()
            val srcAspect = srcWidth / srcHeight
            val dstAspect = width.toFloat() / height.toFloat()

            val baseScale = if (srcAspect > dstAspect) {
                height.toFloat() / srcHeight
            } else {
                width.toFloat() / srcWidth
            }
            val totalScale = baseScale * photoScale
            val finalW = srcWidth * totalScale
            val finalH = srcHeight * totalScale

            val left = (width - finalW) / 2f + (photoOffsetX * width)
            val top = (height - finalH) / 2f + (photoOffsetY * height)

            canvas.save()
            canvas.clipRect(0, 0, width, height)
            val dstRect = RectF(left, top, left + finalW, top + finalH)
            canvas.drawBitmap(customPhotoBitmap, null, dstRect, paint)
            canvas.restore()
            return
        }

        when (preset) {
            PosterPreset.NIGHT -> {
                canvas.drawColor(Color.parseColor("#0D0F12"))
                drawGridLines(canvas, width, height, Color.parseColor("#1F242D"))
            }
            PosterPreset.LIGHT -> {
                canvas.drawColor(Color.parseColor("#F8F9FA"))
                drawGridLines(canvas, width, height, Color.parseColor("#E2E8F0"))
            }
            PosterPreset.TRANSPARENT -> {

            }
            PosterPreset.CUSTOM_PHOTO -> {
                canvas.drawColor(Color.parseColor("#0D0F12"))
            }
        }
    }

    private fun drawGridLines(canvas: Canvas, width: Int, height: Int, color: Int) {
        val scale = width / 360f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = scale
            style = Paint.Style.STROKE
        }
        val step = 40f * scale
        var x = step
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            x += step
        }
        var y = step
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += step
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: PosterData,
        preset: PosterPreset,
        visibility: PosterElementVisibility,
        effects: PosterEffects,
        transform: PosterGroupTransform
    ) {
        canvas.save()
        canvas.translate(transform.offsetX * width, transform.offsetY * height)
        canvas.scale(transform.scale, transform.scale, width / 2f, 32f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val isLight = preset == PosterPreset.LIGHT

        var currentY = 32f
        if (visibility.showAppName) {
            paint.color = if (isLight) Color.parseColor("#00AA13") else Color.parseColor("#00FF66")
            paint.textSize = 16f
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            drawStyledHeaderText(canvas, "DRIVER TRACKER", 16f, currentY, paint, effects)
            currentY += 20f
        }

        if (visibility.showDriverName && data.driverName.isNotBlank()) {
            paint.color = if (isLight) Color.parseColor("#1A202C") else Color.WHITE
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val nameText = "DRIVER: ${data.driverName.uppercase()}"
            drawStyledHeaderText(canvas, nameText, 16f, currentY, paint, effects)
        }

        if (data.dateFormatted.isNotBlank()) {
            paint.color = if (isLight) Color.parseColor("#718096") else Color.argb(180, 255, 255, 255)
            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT
            paint.textAlign = Paint.Align.RIGHT
            drawStyledHeaderText(canvas, data.dateFormatted, width - 16f, 32f, paint, effects)
            paint.textAlign = Paint.Align.LEFT
        }
        canvas.restore()
    }

    private fun drawStyledHeaderText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        fillPaint: Paint,
        effects: PosterEffects
    ) {
        val strokePaint = Paint(fillPaint).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeWidth = effects.textOutlineSize * 2f
            color = parsePosterColor(effects.textOutlineColor, Color.BLACK)
        }
        if (effects.textOutlineEnabled) canvas.drawText(text, x, y, strokePaint)

        if (effects.textShadowEnabled) {
            val shadowColor = parsePosterColor(effects.textShadowColor, Color.BLACK)
            fillPaint.setShadowLayer(
                effects.textShadowSize * 2f,
                effects.textShadowSize * 0.35f,
                effects.textShadowSize,
                Color.argb(120, Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor))
            )
        }
        canvas.drawText(text, x, y, fillPaint)
        fillPaint.clearShadowLayer()
    }

    private fun parsePosterColor(value: String, fallback: Int): Int =
        try { Color.parseColor(value) } catch (_: IllegalArgumentException) { fallback }

    private fun drawRouteTrack(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        gpsPoints: List<GpsPoint>,
        routeLineColor: String = "AUTO",
        effects: PosterEffects,
        routeTransform: PosterGroupTransform,
        emptyStateColor: Int
    ) {
        val routeCenterX = (left + right) / 2f
        val routeCenterY = (top + bottom) / 2f
        canvas.save()
        canvas.translate(routeTransform.offsetX * canvasWidth, routeTransform.offsetY * canvasHeight)
        canvas.scale(routeTransform.scale, routeTransform.scale, routeCenterX, routeCenterY)
        if (gpsPoints.size < 2) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emptyStateColor
                textSize = 14f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
            }
            val message = "Rute GPS belum tersedia"
            val centerX = (left + right) / 2f
            val centerY = (top + bottom) / 2f
            val textWidth = paint.measureText(message)
            paint.color = Color.argb(184, 0, 0, 0)
            canvas.drawRoundRect(
                RectF(centerX - textWidth / 2f - 14f, centerY - 18f, centerX + textWidth / 2f + 14f, centerY + 18f),
                12f,
                12f,
                paint
            )
            paint.color = emptyStateColor
            canvas.drawText(
                message,
                centerX,
                centerY + 5f,
                paint
            )
            canvas.restore()
            return
        }
        val mappedPoints = RouteCanvasProjection.project(
            gpsPoints = gpsPoints,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            sidePaddingFraction = left / canvasWidth,
            topPaddingFraction = top / canvasHeight,
            bottomPaddingFraction = (canvasHeight - bottom) / canvasHeight
        ).map { PointF(it.x, it.y) to it.speed }
        val routeScale = 1f

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val isCustomColor = routeLineColor != "AUTO" && routeLineColor.isNotBlank()
        val customColorParsed = if (isCustomColor) {
            try { Color.parseColor(routeLineColor) } catch (_: Exception) { Color.parseColor("#6FAEB8") }
        } else Color.parseColor("#6FAEB8")

        for (i in 0 until mappedPoints.size - 1) {
            val (p1, speed1) = mappedPoints[i]
            val (p2, speed2) = mappedPoints[i + 1]
            val speed = max(speed1, speed2)

            val color = if (isCustomColor) {
                customColorParsed
            } else {
                when {
                    speed < 15f -> Color.parseColor("#D98983")
                    speed in 15f..30f -> Color.parseColor("#D8B968")
                    else -> Color.parseColor("#6FAEB8")
                }
            }

            if (effects.routeShadowEnabled) {
                val shadowColor = parsePosterColor(effects.routeShadowColor, Color.BLACK)
                val shadowOffset = effects.routeShadowSize * routeScale * 0.3f
                shadowPaint.color = Color.argb(61, Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor))
                shadowPaint.strokeWidth = (9f + effects.routeShadowSize * 0.5f) * routeScale
                canvas.drawLine(p1.x + shadowOffset, p1.y + shadowOffset, p2.x + shadowOffset, p2.y + shadowOffset, shadowPaint)
            }

            if (effects.routeOutlineEnabled) {
                outlinePaint.color = parsePosterColor(effects.routeOutlineColor, Color.WHITE)
                outlinePaint.strokeWidth = (10f + effects.routeOutlineSize * 1.5f) * routeScale
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, outlinePaint)
            }

            linePaint.color = color
            linePaint.strokeWidth = 8f * routeScale
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
            linePaint.color = Color.argb(41, 255, 255, 255)
            linePaint.strokeWidth = 1.2f * routeScale
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
        }

        if (mappedPoints.isNotEmpty()) {
            val startPt = mappedPoints.first().first
            drawPin(canvas, startPt.x, startPt.y, Color.parseColor("#70B99A"), routeScale)
        }

        if (mappedPoints.size > 1) {
            val endPt = mappedPoints.last().first
            drawPin(canvas, endPt.x, endPt.y, Color.parseColor("#D98692"), routeScale)
        }
        canvas.restore()
    }

    private fun drawPin(canvas: Canvas, x: Float, y: Float, color: Int, scale: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.setShadowLayer(2f * scale, 0f, 1f * scale, Color.argb(70, 0, 0, 0))
        paint.color = Color.WHITE
        canvas.drawCircle(x, y, 7.2f * scale, paint)
        paint.clearShadowLayer()
        paint.color = color
        canvas.drawCircle(x, y, 5.2f * scale, paint)
    }

    private fun drawGamificationBadges(
        canvas: Canvas,
        width: Int,
        badgeY: Float,
        data: PosterData
    ) {
        val badges = mutableListOf<String>()

        if (data.distanceKm >= 100.0) {
            badges.add("Jarak 100 km")
        } else if (data.distanceKm >= 50.0) {
            badges.add("Jarak 50 km")
        }

        if (data.avgSpeedKmH >= 25f) {
            badges.add("Kurir tercepat")
        } else if (data.avgSpeedKmH > 0) {
            badges.add("Pengendara dalam kota")
        }

        if (data.netProfit >= 150000.0) {
            badges.add("Pendapatan tertinggi")
        }

        if (data.orderCount >= 5) {
            badges.add("Pesanan terbanyak")
        }

        if (badges.isEmpty()) {
            badges.add("⭐ Star Driver")
        }

        var startX = 60f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (badge in badges.take(3)) {
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val textWidth = paint.measureText(badge)
            val pillWidth = textWidth + 36f
            val pillHeight = 40f

            if (startX + pillWidth > width - 60f) break

            val rect = RectF(startX, badgeY, startX + pillWidth, badgeY + pillHeight)
            paint.color = Color.argb(180, 0, 230, 118)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 20f, 20f, paint)

            paint.color = Color.parseColor("#00FF66")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rect, 20f, 20f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(badge, startX + pillWidth / 2f, badgeY + 26f, paint)

            startX += pillWidth + 16f
        }
    }

    private fun drawTelemetryCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        data: PosterData,
        visibility: PosterElementVisibility,
        transform: PosterGroupTransform,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val hasTopStats = visibility.showDistance || visibility.showIncome
        val hasSubStats = visibility.showDuration || visibility.showAvgSpeed || visibility.showMaxSpeed

        if (!hasTopStats && !hasSubStats) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rect = RectF(left, top, right, bottom)
        canvas.save()
        canvas.translate(transform.offsetX * canvasWidth, transform.offsetY * canvasHeight)
        canvas.scale(transform.scale, transform.scale, (left + right) / 2f, (top + bottom) / 2f)
        paint.color = Color.argb(221, 15, 20, 28)
        paint.style = Paint.Style.FILL
        paint.setShadowLayer(4f, 0f, 2f, Color.argb(85, 0, 0, 0))
        canvas.drawRoundRect(rect, 16f, 16f, paint)
        paint.clearShadowLayer()
        paint.style = Paint.Style.FILL

        val contentLeft = left + 12f
        val contentRight = right - 12f
        val contentWidth = contentRight - contentLeft
        var cursorY = top + 12f

        if (visibility.showDistance) {
            paint.color = Color.parseColor("#00FF66")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val distText = String.format(Locale.US, "%.1f KM", data.distanceKm)
            canvas.drawText(distText, contentLeft, cursorY + 19f, paint)

            paint.color = Color.argb(180, 255, 255, 255)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("TOTAL JARAK", contentLeft, cursorY + 31f, paint)
        }

        if (visibility.showIncome) {
            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            val profitStr = data.netProfit.toRupiahString()
            canvas.drawText(profitStr, contentRight, cursorY + 19f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        if (hasSubStats) {
            if (hasTopStats) {
                val dividerY = top + 53f
                paint.color = Color.argb(38, 255, 255, 255)
                paint.strokeWidth = 1f
                canvas.drawLine(contentLeft, dividerY, contentRight, dividerY, paint)
                cursorY = dividerY + 6f
            }

            val activeSubStats = mutableListOf<String>()
            if (visibility.showDuration) {
                val h = data.durationSeconds / 3600
                val m = (data.durationSeconds % 3600) / 60
                val durText = if (h > 0) String.format(Locale.US, "%dh %dm", h, m) else String.format(Locale.US, "%dm", m)
                activeSubStats.add("Durasi $durText")
            }
            if (visibility.showAvgSpeed) {
                activeSubStats.add(String.format(Locale.US, "Kecepatan rata-rata %.1f km/j", data.avgSpeedKmH))
            }
            if (visibility.showMaxSpeed) {
                activeSubStats.add(String.format(Locale.US, "Kecepatan maksimum %.1f km/j", data.maxSpeedKmH))
            }

            if (activeSubStats.isNotEmpty()) {
                paint.color = Color.WHITE
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val widths = activeSubStats.map { paint.measureText(it) }
                val totalWidth = widths.sum()
                val gap = if (activeSubStats.size > 1) {
                    ((contentWidth - totalWidth) / (activeSubStats.size - 1)).coerceAtLeast(0f)
                } else 0f
                var x = contentLeft
                activeSubStats.forEachIndexed { index, text ->
                    canvas.drawText(text, x, cursorY + 11f, paint)
                    x += widths[index] + gap
                }
            }
        }
        canvas.restore()
    }

}
