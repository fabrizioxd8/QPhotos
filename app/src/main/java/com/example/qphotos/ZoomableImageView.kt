package com.example.qphotos

import android.content.Context

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

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

    private var oldMeasuredWidth = 0
    private var oldMeasuredHeight = 0


    private var mScaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private var initialMatrix = Matrix()

    val isZoomed: Boolean
        get() = saveScale > minScale

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

    fun resetToInitialState() {

        matrix_.set(initialMatrix)
        saveScale = minScale
        imageMatrix = matrix_
        fixTrans()
        invalidate()
    }


    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (isZoomed) {
                resetZoom()

    fun prepareForNewImage() {
        saveScale = 1f
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (isZoomed) {
                resetToInitialState()

            }
            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
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

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
                matrix_.postScale(mScaleFactor, mScaleFactor, viewWidth / 2f, viewHeight / 2f)
            } else {
                matrix_.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
            }
            fixTrans()
            return true
        }
    }

    private fun fixTrans() {
        matrix_.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]
        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f) {
            matrix_.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans) return -trans + minTrans
        return if (trans > maxTrans) -trans + maxTrans else 0f
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {

        return if (contentSize <= viewSize) 0f else delta
    }

    private fun fitToScreen() {
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
        matrix_.postTranslate(redundantXSpace / 2, redundantYSpace / 2)

        initialMatrix.set(matrix_)
        imageMatrix = matrix_
        fixTrans()
        invalidate()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        fitToScreen()

        return if (contentSize <= viewSize) {
            0f
        } else delta

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

        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight || viewWidth == 0 || viewHeight == 0) {
            return
        }
        oldMeasuredHeight = viewHeight
        oldMeasuredWidth = viewWidth
        if (saveScale == 1f) {
            val scale: Float
            val d = drawable
            if (d == null) {
                return
            }
            val bmWidth = d.intrinsicWidth
            val bmHeight = d.intrinsicHeight
            val scaleX = viewWidth.toFloat() / bmWidth.toFloat()
            val scaleY = viewHeight.toFloat() / bmHeight.toFloat()
            scale = scaleX.coerceAtMost(scaleY)
            matrix_.setScale(scale, scale)
            minScale = scale
            saveScale = minScale

            // Center the image
            var redundantYSpace = viewHeight.toFloat() - scale * bmHeight.toFloat()
            var redundantXSpace = viewWidth.toFloat() - scale * bmWidth.toFloat()
            redundantYSpace /= 2f
            redundantXSpace /= 2f
            matrix_.postTranslate(redundantXSpace, redundantYSpace)
            origWidth = viewWidth - 2 * redundantXSpace
            origHeight = viewHeight - 2 * redundantYSpace
            imageMatrix = matrix_
            initialMatrix.set(matrix_)
        }
        fixTrans()

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        val curr = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                last.set(curr)
                start.set(last)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    parent.requestDisallowInterceptTouchEvent(isZoomed)
                    val deltaX = curr.x - last.x
                    val deltaY = curr.y - last.y
                    val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * saveScale)
                    val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * saveScale)
                    matrix_.postTranslate(fixTransX, fixTransY)
                    fixTrans()
                    last.set(curr.x, curr.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                parent.requestDisallowInterceptTouchEvent(false)
                mode = NONE
                val xDiff = Math.abs(curr.x - start.x).toInt()
                val yDiff = Math.abs(curr.y - start.y).toInt()
                if (xDiff < CLICK && yDiff < CLICK) performClick()
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
        const val CLICK = 3
    }
}