package com.deploydulupulangnanti.videodownloaderpremium

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Original vector-like tutorial images. No network, bitmap text, or fixed-language labels. */
class TutorialImage(context: Context, private val step: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ink = Color.rgb(100, 226, 186)
    init { contentDescription = context.getString(R.string.accessibility_tutorial) }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(width / 360f, height / 190f)
        fun box(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float = 12f) {
            paint.color = color; paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(x, y, x + w, y + h), radius, radius, paint)
        }
        fun line(x: Float, y: Float, x2: Float, y2: Float, color: Int, stroke: Float = 5f) {
            paint.color = color; paint.strokeWidth = stroke; paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(x, y, x2, y2, paint)
        }
        box(0f, 0f, 360f, 190f, Color.rgb(25, 45, 38), 20f)
        box(76f, 15f, 208f, 165f, Color.rgb(12, 24, 19), 18f)
        box(88f, 28f, 184f, 22f, Color.rgb(43, 62, 53), 8f)
        line(114f, 39f, 231f, 39f, Color.rgb(126, 154, 141), 3f)
        when (step) {
            1 -> {
                paint.color = ink; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
                canvas.drawCircle(100f, 38f, 4f, paint)
                line(103f, 41f, 107f, 45f, ink, 2f)
                repeat(3) { index ->
                    val y = 68f + index * 32f
                    box(91f, y, 25f, 23f, Color.rgb(45, 77, 62), 6f)
                    line(128f, y + 5f, 244f, y + 5f, ink, 3f)
                    line(128f, y + 15f, 218f, y + 15f, Color.rgb(93, 125, 110), 3f)
                }
            }
            2 -> {
                box(90f, 61f, 180f, 83f, Color.rgb(44, 77, 64), 8f)
                paint.color = ink
                canvas.drawPath(Path().apply { moveTo(167f, 82f); lineTo(167f, 124f); lineTo(202f, 103f); close() }, paint)
                box(181f, 151f, 87f, 17f, ink, 6f)
                line(296f, 144f, 266f, 159f, Color.WHITE, 3f)
                paint.color = Color.WHITE; canvas.drawCircle(299f, 143f, 5f, paint)
            }
            3 -> {
                box(93f, 65f, 174f, 93f, Color.rgb(32, 49, 40), 9f)
                line(107f, 82f, 249f, 82f, Color.rgb(126, 154, 141), 4f)
                box(107f, 100f, 12f, 12f, ink, 3f)
                line(130f, 106f, 237f, 106f, Color.rgb(126, 154, 141), 3f)
                box(108f, 128f, 144f, 18f, ink, 6f)
                line(180f, 130f, 180f, 140f, Color.rgb(16, 35, 29), 3f)
                line(175f, 137f, 180f, 142f, Color.rgb(16, 35, 29), 3f)
                line(185f, 137f, 180f, 142f, Color.rgb(16, 35, 29), 3f)
            }
            else -> repeat(3) { index ->
                val y = 65f + index * 32f
                box(92f, y, 33f, 25f, Color.rgb(44, 77, 64), 5f)
                paint.color = ink
                canvas.drawPath(Path().apply { moveTo(103f, y + 7f); lineTo(103f, y + 19f); lineTo(115f, y + 13f); close() }, paint)
                line(138f, y + 5f, 245f, y + 5f, Color.rgb(126, 154, 141), 3f)
                line(138f, y + 17f, 236f, y + 17f, if (index == 0) ink else Color.rgb(52, 91, 73), 4f)
            }
        }
        canvas.restore()
    }
}
