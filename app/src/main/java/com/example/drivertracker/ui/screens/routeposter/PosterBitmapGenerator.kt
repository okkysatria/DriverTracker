package com.example.drivertracker.ui.screens.routeposter

import android.content.Context
import android.graphics.*
import com.example.drivertracker.data.model.GpsPoint
import com.example.drivertracker.ui.utils.toRupiahString
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

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
        routeLineColor: String = "AUTO"
    ): Bitmap {
        val width = aspectRatio.width
        val height = aspectRatio.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas, width, height, preset, customPhotoBitmap, photoScale, photoOffsetX, photoOffsetY)

        drawHeader(canvas, width, data, preset, visibility)

        val isStory = aspectRatio == PosterAspectRatio.STORY_9_16
        val padX = width * 0.08f
        val padTop = if (isStory) height * 0.14f else height * 0.15f
        val padBottom = if (isStory) height * 0.22f else height * 0.24f

        if (visibility.showRouteLine) {
            drawRouteTrack(
                canvas = canvas,
                left = padX,
                top = padTop,
                right = width - padX,
                bottom = height - padBottom,
                gpsPoints = data.gpsPoints,
                routeLineColor = routeLineColor,
                emptyStateColor = if (preset == PosterPreset.LIGHT) {
                    Color.rgb(100, 116, 139)
                } else {
                    Color.argb(170, 255, 255, 255)
                }
            )
        }

        val hasTopStats = visibility.showDistance || visibility.showIncome
        val hasSubStats = visibility.showDuration || visibility.showAvgSpeed || visibility.showMaxSpeed
        val cardHeight = if (hasSubStats) (if (isStory) 230f else 190f) else (if (isStory) 130f else 110f)
        val cardMarginBottom = if (isStory) 60f else 40f
        val statsTop = height - cardHeight - cardMarginBottom

        drawTelemetryCard(
            canvas = canvas,
            left = 40f,
            top = statsTop,
            right = width - 40f,
            bottom = statsTop + cardHeight,
            data = data,
            visibility = visibility
        )

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

            paint.color = Color.argb(128, 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
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
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val step = 80f
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
        width: Int,
        data: PosterData,
        preset: PosterPreset,
        visibility: PosterElementVisibility
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val isLight = preset == PosterPreset.LIGHT

        var currentY = 90f
        if (visibility.showAppName) {
            paint.color = if (isLight) Color.parseColor("#00AA13") else Color.parseColor("#00FF66")
            paint.textSize = 32f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DRIVER TRACKER", 60f, currentY, paint)
            currentY += 40f
        }

        if (visibility.showDriverName && data.driverName.isNotBlank()) {
            paint.color = if (isLight) Color.parseColor("#1A202C") else Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val nameText = "DRIVER: ${data.driverName.uppercase()}"
            canvas.drawText(nameText, 60f, currentY, paint)
        }

        if (data.dateFormatted.isNotBlank()) {
            paint.color = if (isLight) Color.parseColor("#718096") else Color.argb(180, 255, 255, 255)
            paint.textSize = 20f
            paint.typeface = Typeface.DEFAULT
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(data.dateFormatted, (width - 60).toFloat(), 90f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawRouteTrack(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        gpsPoints: List<GpsPoint>,
        routeLineColor: String = "AUTO",
        emptyStateColor: Int
    ) {
        if (gpsPoints.size < 2) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emptyStateColor
                textSize = 30f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
            }
            canvas.drawText(
                "RUTE GPS BELUM TERSEDIA",
                (left + right) / 2f,
                (top + bottom) / 2f,
                paint
            )
            return
        }
        val points = gpsPoints

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE

        for (pt in points) {
            if (pt.lat < minLat) minLat = pt.lat
            if (pt.lat > maxLat) maxLat = pt.lat
            if (pt.lng < minLng) minLng = pt.lng
            if (pt.lng > maxLng) maxLng = pt.lng
        }

        val latDiff = max(maxLat - minLat, 0.0001)
        val lngDiff = max(maxLng - minLng, 0.0001)

        val rectW = right - left
        val rectH = bottom - top

        val scaleX = rectW / lngDiff
        val scaleY = rectH / latDiff
        val scale = min(scaleX, scaleY)

        val trackW = lngDiff * scale
        val trackH = latDiff * scale

        val offsetX = left + (rectW - trackW) / 2f
        val offsetY = top + (rectH - trackH) / 2f

        val mappedPoints = points.map { pt ->
            val x = offsetX + (pt.lng - minLng) * scale

            val y = offsetY + (maxLat - pt.lat) * scale
            PointF(x.toFloat(), y.toFloat()) to pt.speed
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val isCustomColor = routeLineColor != "AUTO" && routeLineColor.isNotBlank()
        val customColorParsed = if (isCustomColor) {
            try { Color.parseColor(routeLineColor) } catch (_: Exception) { Color.parseColor("#00E5FF") }
        } else Color.parseColor("#00E5FF")

        for (i in 0 until mappedPoints.size - 1) {
            val (p1, speed1) = mappedPoints[i]
            val (p2, speed2) = mappedPoints[i + 1]
            val speed = max(speed1, speed2)

            val color = if (isCustomColor) {
                customColorParsed
            } else {
                when {
                    speed < 15f -> Color.parseColor("#FF3D00")
                    speed in 15f..30f -> Color.parseColor("#FFD600")
                    else -> Color.parseColor("#00E5FF")
                }
            }

            glowPaint.color = Color.argb(105, 0, 0, 0)
            glowPaint.strokeWidth = 10f
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, glowPaint)

            if (isCustomColor || speed > 30f) {
                glowPaint.color = if (isCustomColor) {
                    Color.argb(55, Color.red(customColorParsed), Color.green(customColorParsed), Color.blue(customColorParsed))
                } else {
                    Color.argb(55, 34, 211, 238)
                }
                glowPaint.strokeWidth = 14f
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, glowPaint)
            }

            linePaint.color = color
            linePaint.strokeWidth = 6f
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
        }

        if (mappedPoints.isNotEmpty()) {
            val startPt = mappedPoints.first().first
            drawPin(canvas, startPt.x, startPt.y, "A", Color.parseColor("#00E676"))
        }

        if (mappedPoints.size > 1) {
            val endPt = mappedPoints.last().first
            drawPin(canvas, endPt.x, endPt.y, "B", Color.parseColor("#FF1744"))
        }
    }

    private fun drawPin(canvas: Canvas, x: Float, y: Float, label: String, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawCircle(x, y, 18f, paint)

        paint.color = color
        canvas.drawCircle(x, y, 12f, paint)

        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, x, y + 4.5f, paint)
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
        visibility: PosterElementVisibility
    ) {
        val hasTopStats = visibility.showDistance || visibility.showIncome
        val hasSubStats = visibility.showDuration || visibility.showAvgSpeed || visibility.showMaxSpeed

        if (!hasTopStats && !hasSubStats) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val actualBottom = if (hasSubStats) bottom else top + 130f
        val rect = RectF(left, top, right, actualBottom)
        paint.color = Color.argb(210, 15, 20, 28)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 32f, 32f, paint)

        paint.color = Color.argb(60, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rect, 32f, 32f, paint)

        paint.style = Paint.Style.FILL

        if (visibility.showDistance) {
            paint.color = Color.parseColor("#00FF66")
            paint.textSize = 50f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val distText = String.format(Locale.US, "%.1f KM", data.distanceKm)
            canvas.drawText(distText, left + 36f, top + 66f, paint)

            paint.color = Color.argb(180, 255, 255, 255)
            paint.textSize = 16f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("TOTAL JARAK", left + 36f, top + 98f, paint)
        }

        if (visibility.showIncome) {
            paint.color = Color.WHITE
            paint.textSize = 34f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            val profitStr = data.netProfit.toRupiahString()
            canvas.drawText(profitStr, right - 36f, top + 74f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        if (hasSubStats) {
            val dividerY = top + 120f
            if (hasTopStats) {
                paint.color = Color.argb(40, 255, 255, 255)
                paint.strokeWidth = 2f
                canvas.drawLine(left + 36f, dividerY, right - 36f, dividerY, paint)
            }

            val activeSubStats = mutableListOf<Pair<String, String>>()
            if (visibility.showDuration) {
                val h = data.durationSeconds / 3600
                val m = (data.durationSeconds % 3600) / 60
                val durText = if (h > 0) String.format(Locale.US, "%dh %dm", h, m) else String.format(Locale.US, "%dm", m)
                activeSubStats.add(durText to "DURASI")
            }
            if (visibility.showAvgSpeed) {
                activeSubStats.add(String.format(Locale.US, "%.1f km/j", data.avgSpeedKmH) to "RATA-RATA")
            }
            if (visibility.showMaxSpeed) {
                activeSubStats.add(String.format(Locale.US, "%.1f km/j", data.maxSpeedKmH) to "KECEPATAN MAKSIMUM")
            }

            if (activeSubStats.isNotEmpty()) {
                val colWidth = (right - left - 72f) / activeSubStats.size
                val statY = dividerY + 48f
                val labelY = dividerY + 76f

                activeSubStats.forEachIndexed { idx, (value, label) ->
                    drawStatColumn(canvas, left + 36f + (colWidth * idx), statY, labelY, value, label)
                }
            }
        }
    }

    private fun drawStatColumn(
        canvas: Canvas,
        x: Float,
        statY: Float,
        labelY: Float,
        valText: String,
        lblText: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(valText, x, statY, paint)

        paint.color = Color.argb(160, 255, 255, 255)
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(lblText, x, labelY, paint)
    }

}
