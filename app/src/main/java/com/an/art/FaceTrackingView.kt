package com.an.art

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceTrackingView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val paint: Paint = Paint()
    private val faces = mutableListOf<Face>()

    init {
        paint.color = Color.RED
        paint.strokeWidth = 10f
        paint.style = Paint.Style.STROKE
    }

    fun updateFaces(faces: List<Face>) {
        this.faces.clear()
        this.faces.addAll(faces)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return
        faces.forEach {
            canvas.drawRect(it.boundingBox, paint)
        }
    }
}