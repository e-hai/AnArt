package com.an.art.demo_opencv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet

class FaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private val pointPaint = Paint()
    private val facePoint = mutableListOf<PointF>()

    init {
        pointPaint.color = Color.RED
        pointPaint.style = Paint.Style.FILL_AND_STROKE
        pointPaint.strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        facePoint.forEach {
            val x = it.x * width
            val y = it.y * height
            canvas?.drawPoint(x, y, pointPaint)
        }
    }

    fun updateFacePoints(points: List<PointF>) {
        facePoint.clear()
        facePoint.addAll(points)
        invalidate()
    }
}