package com.example.drivertracker.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

fun createMapPinDrawable(
    context: Context,
    @ColorInt color: Int,
    glyph: MapPinGlyph
): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (44f * density).toInt().coerceAtLeast(44)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = size / 44f
    canvas.save()
    canvas.scale(scale, scale)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
        setShadowLayer(2.5f, 0f, 2f, 0x55000000)
    }
    val whiteStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeJoin = Paint.Join.ROUND
        this.color = android.graphics.Color.WHITE
    }
    val pin = Path().apply {
        moveTo(22f, 42f)
        cubicTo(18f, 35f, 7f, 24f, 7f, 17f)
        cubicTo(7f, 8.7f, 13.7f, 2f, 22f, 2f)
        cubicTo(30.3f, 2f, 37f, 8.7f, 37f, 17f)
        cubicTo(37f, 24f, 26f, 35f, 22f, 42f)
        close()
    }
    canvas.drawPath(pin, fill)
    fill.clearShadowLayer()
    canvas.drawPath(pin, whiteStroke)

    val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    when (glyph) {
        MapPinGlyph.PASSENGER -> {
            canvas.drawCircle(22f, 13f, 3.2f, glyphPaint)
            canvas.drawArc(RectF(15.5f, 17f, 28.5f, 29f), 190f, 160f, false, glyphPaint)
            canvas.drawLine(22f, 18f, 22f, 26f, glyphPaint)
            canvas.drawLine(18f, 21f, 26f, 21f, glyphPaint)
        }
        MapPinGlyph.FOOD -> {
            canvas.drawLine(16f, 10f, 16f, 24f, glyphPaint)
            canvas.drawLine(13.5f, 10f, 13.5f, 15f, glyphPaint)
            canvas.drawLine(18.5f, 10f, 18.5f, 15f, glyphPaint)
            canvas.drawLine(16f, 24f, 16f, 28f, glyphPaint)
            canvas.drawOval(RectF(24f, 10f, 29f, 18f), glyphPaint)
            canvas.drawLine(26.5f, 18f, 26.5f, 28f, glyphPaint)
        }
        MapPinGlyph.PACKAGE -> {
            val box = Path().apply {
                moveTo(14f, 14f); lineTo(22f, 10f); lineTo(30f, 14f)
                lineTo(30f, 24f); lineTo(22f, 28f); lineTo(14f, 24f); close()
                moveTo(22f, 10f); lineTo(22f, 20f); lineTo(30f, 14f)
                moveTo(22f, 20f); lineTo(14f, 14f)
            }
            canvas.drawPath(box, glyphPaint)
        }
        MapPinGlyph.DRIVER -> {
            val arrow = Path().apply {
                moveTo(22f, 9f); lineTo(29f, 26f); lineTo(22f, 22f)
                lineTo(15f, 26f); close()
            }
            glyphPaint.style = Paint.Style.FILL
            canvas.drawPath(arrow, glyphPaint)
        }
        MapPinGlyph.PREDICTION -> {
            glyphPaint.style = Paint.Style.FILL
            canvas.drawCircle(22f, 18f, 6f, glyphPaint)
            glyphPaint.style = Paint.Style.STROKE
            glyphPaint.strokeWidth = 2f
            canvas.drawCircle(22f, 18f, 10f, glyphPaint)
        }
    }
    canvas.restore()
    return BitmapDrawable(context.resources, bitmap).apply {
        setBounds(0, 0, size, size)
    }
}

enum class MapPinGlyph { PASSENGER, FOOD, PACKAGE, DRIVER, PREDICTION }

fun orderMapPinGlyph(type: String): MapPinGlyph = when (type.trim().lowercase()) {
    "makanan", "food" -> MapPinGlyph.FOOD
    "paket", "package" -> MapPinGlyph.PACKAGE
    else -> MapPinGlyph.PASSENGER
}

@ColorInt
fun orderMapPinColor(type: String): Int = when (orderMapPinGlyph(type)) {
    MapPinGlyph.FOOD -> android.graphics.Color.rgb(220, 38, 38)
    MapPinGlyph.PACKAGE -> android.graphics.Color.rgb(37, 99, 235)
    else -> android.graphics.Color.rgb(22, 163, 74)
}
