package com.example.pathfinder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CustomOverlay(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 10f
        style = Paint.Style.FILL
    }
    private val keypoints = mutableListOf<Pair<Float, Float>>()

    fun setKeypoints(newKeypoints: List<Pair<Float, Float>>) {
        keypoints.clear()
        keypoints.addAll(newKeypoints)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (keypoint in keypoints) {
            canvas.drawCircle(keypoint.first, keypoint.second, 10f, paint)
        }
    }
}
