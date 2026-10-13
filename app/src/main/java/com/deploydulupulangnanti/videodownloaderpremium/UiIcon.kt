package com.deploydulupulangnanti.videodownloaderpremium

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Original line icons, rendered at the device density without font glyph dependencies. */
class UiIcon(context: Context, private val kind: String, private val tint: Int) : View(context) {
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint; style = Paint.Style.STROKE; strokeWidth = 1.8f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas) {
        val save = canvas.save()
        val size = minOf(width, height).toFloat()
        canvas.translate((width - size) / 2, (height - size) / 2); canvas.scale(size / 24, size / 24)
        fun line(vararg points: Float) { val path = Path(); path.moveTo(points[0], points[1]); for (i in 2 until points.size step 2) path.lineTo(points[i], points[i + 1]); canvas.drawPath(path, pen) }
        when (kind) {
            "browse" -> { canvas.drawCircle(12f,12f,9f,pen); canvas.drawOval(8f,3f,16f,21f,pen); line(3f,12f,21f,12f); line(5f,7f,19f,7f); line(5f,17f,19f,17f) }
            "downloads" -> { line(12f,3f,12f,15f); line(7f,10f,12f,15f,17f,10f); line(4f,16f,4f,21f,20f,21f,20f,16f) }
            "guide" -> { canvas.drawCircle(12f,12f,9f,pen); val p = Path(); p.moveTo(9f,8f); p.cubicTo(9f,4f,17f,5f,15f,10f); p.lineTo(12f,13f); p.lineTo(12f,14f); canvas.drawPath(p,pen); canvas.drawPoint(12f,18f,pen) }
            "settings" -> { canvas.drawCircle(12f,12f,3f,pen); canvas.drawCircle(12f,12f,7f,pen); for(i in 0..7) { canvas.save(); canvas.rotate(i*45f,12f,12f); line(12f,2f,12f,5f); canvas.restore() } }
            "tabs" -> { canvas.drawRoundRect(6f,3f,21f,18f,2f,2f,pen); line(3f,7f,3f,21f,17f,21f) }
            "history" -> { canvas.drawArc(3f,3f,21f,21f,-50f,300f,false,pen); line(3f,3f,3f,9f,9f,9f); line(12f,7f,12f,12f,16f,14f) }
            "add" -> { line(12f,4f,12f,20f); line(4f,12f,20f,12f) }
            "close" -> { line(6f,6f,18f,18f); line(18f,6f,6f,18f) }
            "back" -> line(15f,5f,8f,12f,15f,19f)
            "forward" -> line(9f,5f,16f,12f,9f,19f)
            "home" -> { line(3f,11f,12f,3f,21f,11f); line(6f,9f,6f,21f,18f,21f,18f,9f); line(10f,21f,10f,15f,14f,15f,14f,21f) }
            "bookmark" -> line(6f,21f,6f,3f,18f,3f,18f,21f,12f,17f,6f,21f)
            "reload" -> { canvas.drawArc(4f,4f,20f,20f,30f,300f,false,pen); line(20f,4f,20f,10f,14f,10f) }
            "search" -> { canvas.drawCircle(10f,10f,6f,pen); line(15f,15f,21f,21f) }
            "menu" -> { canvas.drawPoint(12f,5f,pen); canvas.drawPoint(12f,12f,pen); canvas.drawPoint(12f,19f,pen) }
        }
        canvas.restoreToCount(save)
    }
}
