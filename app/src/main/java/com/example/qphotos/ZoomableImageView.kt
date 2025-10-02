package com.example.qphotos

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView

import kotlin.math.abs


class ZoomableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var matrix_ = Matrix()
    private var mode = NONE

    private var last = PointF()
    private var start = PointF()
    private var minScale = 1f
    private var maxScale = 4f
    private var m: FloatArray

    private var viewWidth = 0
    private var viewHeight = 0
    private var saveScale = 1f
    private var origWidth = 0f
    private var origHeight = 0f

    private var mScaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private var currentAnimator: ValueAnimator? = null

    private val isZoomed: Boolean

        get() = saveScale > minScale + 0.01f


    init {
        super.setClickable(true)
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
        matrix_.setTranslate(1f, 1f)
        m = FloatArray(9)
        imageMatrix = matrix_
        scaleType = ScaleType.MATRIX
    }

    fun resetZoom() {
        fitToScreen()
        invalidate()
    }

    private fun animateZoom(targetScale: Float, focusX: Float, focusY: Float) {
        currentAnimator?.cancel()


        val startScale = saveScale
        val endScale = targetScale.coerceIn(minScale, maxScale)

        currentAnimator = ValueAnimator.ofFloat(startScale, endScale).apply {

            interpolator = AccelerateDecelerateInterpolator()
            duration = 300
            addUpdateListener { animation ->
                val newScale = animation.animatedValue as Float
                val scaleFactor = newScale / saveScale
                saveScale = newScale

                matrix_.postScale(scaleFactor, scaleFactor, focusX, focusY)
                fixTrans()
                imageMatrix = matrix_
            }
            start()
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (isZoomed) {
                animateZoom(minScale, e.x, e.y)
            } else {
                animateZoom(minScale * 2.5f, e.x, e.y)
            }
            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            currentAnimator?.cancel()
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= mScaleFactor
            if (saveScale > maxScale) {
                saveScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                mScaleFactor = minScale / origScale
            }


            matrix_.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)

            fixTrans()
            return true
        }
    }

    private fun fixTrans() {
        matrix_.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]


        val contentWidth = origWidth * saveScale
        val contentHeight = origHeight * saveScale

        val fixTransX = when {
            contentWidth < viewWidth -> (viewWidth - contentWidth) / 2 - transX
            transX > 0 -> -transX
            transX < viewWidth - contentWidth -> viewWidth - contentWidth - transX
            else -> 0f
        }

        val fixTransY = when {
            contentHeight < viewHeight -> (viewHeight - contentHeight) / 2 - transY
            transY > 0 -> -transY
            transY < viewHeight - contentHeight -> viewHeight - contentHeight - transY
            else -> 0f
        }

        if (fixTransX != 0f || fixTransY != 0f) {
            matrix_.postTranslate(fixTransX, fixTransY)
        }

    }

    private fun fitToScreen() {
        currentAnimator?.cancel()
        val d = drawable ?: return
        val bmWidth = d.intrinsicWidth
        val bmHeight = d.intrinsicHeight
        if (bmWidth == 0 || bmHeight == 0 || viewWidth == 0 || viewHeight == 0) return

        val scaleX = viewWidth.toFloat() / bmWidth.toFloat()
        val scaleY = viewHeight.toFloat() / bmHeight.toFloat()
        val scale = scaleX.coerceAtMost(scaleY)

        matrix_.reset()
        matrix_.setScale(scale, scale)
        minScale = scale
        saveScale = scale

        val redundantYSpace = viewHeight.toFloat() - scale * bmHeight.toFloat()
        val redundantXSpace = viewWidth.toFloat() - scale * bmWidth.toFloat()
        origWidth = viewWidth - redundantXSpace
        origHeight = viewHeight - redundantYSpace
        matrix_.postTranslate(redundantXSpace / 2, redundantYSpace / 2)

        imageMatrix = matrix_

    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        fitToScreen()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val newWidth = MeasureSpec.getSize(widthMeasureSpec)
        val newHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (newWidth != viewWidth || newHeight != viewHeight) {
            viewWidth = newWidth
            viewHeight = newHeight
            fitToScreen()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val curr = PointF(event.x, event.y)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {

                mode = DRAG
                last.set(curr)
                start.set(last)
                parent.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val deltaX = curr.x - last.x
                    val deltaY = curr.y - last.y
                    matrix_.postTranslate(deltaX, deltaY)
                    fixTrans()
                    last.set(curr.x, curr.y)
                }
                parent.requestDisallowInterceptTouchEvent(isZoomed || mode == ZOOM)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = NONE
                parent.requestDisallowInterceptTouchEvent(false)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = ZOOM
            }

            MotionEvent.ACTION_POINTER_UP -> {
                mode = DRAG
            }
        }

        imageMatrix = matrix_
        invalidate()
        return true
    }

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
    }
}