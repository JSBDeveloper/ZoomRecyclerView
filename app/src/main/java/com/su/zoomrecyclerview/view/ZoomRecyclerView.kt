package com.su.zoomrecyclerview.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import com.su.zoomrecyclerview.R

class ZoomRecyclerView: RecyclerView {

    // 기본 세팅 값
    private val DEFAULT_SCALE_DURATION = 300
    private val DEFAULT_SCALE_FACTOR = 1f
    private val DEFAULT_MAX_SCALE_FACTOR = 2.0f
    private val DEFAULT_MIN_SCALE_FACTOR = 0.5f
    private val PROPERTY_SCALE = "scale"
    private val PROPERTY_TRANX = "tranX"
    private val PROPERTY_TRANY = "tranY"
    private val INVALID_TOUCH_POSITION = -1f

    // touch detector
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetectorCompat? = null

    // draw param
    private var mViewWidth: Float = 0f
    private var mViewHeight: Float = 0f
    private var mTranX: Float = 0f
    private var mTranY: Float = 0f
    private var mScaleFactor: Float = 0f

    // touch param
    private var mActivePointerId = INVALID_POINTER_ID
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f

    // control param
    private var isScaling = false
    private var isEnableScale = false

    // zoom param
    private var mScaleAnimator: ValueAnimator? = null
    private var mScaleCenterX: Float = 0f
    private var mScaleCenterY: Float = 0f
    private var mMaxTranX: Float = 0f
    private var mMaxTranY: Float = 0f

    // config param
    private var mMaxScaleFactor: Float = 0f
    private var mMinScaleFactor: Float = 0f
    private var mDefaultScaleFactor: Float = 0f
    private var mScaleDuration: Int = 0

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?):super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int):super(context, attrs, defStyle) {
        init(attrs)
    }

    private fun init(attr: AttributeSet?) {
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetectorCompat(context, GestureListener())

        if (attr != null) {
            val a = context.obtainStyledAttributes(attr, R.styleable.ZoomRecyclerView, 0, 0)
            mMinScaleFactor = a.getFloat(R.styleable.ZoomRecyclerView_min_scale, DEFAULT_MIN_SCALE_FACTOR)
            mMaxScaleFactor = a.getFloat(R.styleable.ZoomRecyclerView_max_scale, DEFAULT_MAX_SCALE_FACTOR)
            mDefaultScaleFactor = a.getFloat(R.styleable.ZoomRecyclerView_default_scale, DEFAULT_SCALE_FACTOR)
            mScaleFactor = mDefaultScaleFactor
            mScaleDuration = a.getInteger(R.styleable.ZoomRecyclerView_zoom_duration, DEFAULT_SCALE_DURATION)
            a.recycle()
        } else {
            //init param with default
            mMaxScaleFactor = DEFAULT_MAX_SCALE_FACTOR
            mMinScaleFactor = DEFAULT_MIN_SCALE_FACTOR
            mDefaultScaleFactor = DEFAULT_SCALE_FACTOR
            mScaleFactor = mDefaultScaleFactor
            mScaleDuration = DEFAULT_SCALE_DURATION
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        mViewWidth = MeasureSpec.getSize(widthSpec).toFloat()
        mViewHeight = MeasureSpec.getSize(heightSpec).toFloat()
        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (!isEnableScale) {
            return super.onTouchEvent(e)
        }

        var retVal = mScaleDetector!!.onTouchEvent(e)
        retVal = mGestureDetector!!.onTouchEvent(e) || retVal

        val action = e!!.getActionMasked()

        when(action) {
            ACTION_DOWN -> {
                val pointerIndex = e.getActionIndex()
                val x = e.getX(pointerIndex)
                val y = e.getY(pointerIndex)
                // Remember where we started (for dragging)
                mLastTouchX = x
                mLastTouchY = y
                // Save the ID of this pointer (for dragging)
                mActivePointerId = e.getPointerId(0)
            }

            ACTION_MOVE -> {
                try {
                    // Find the index of the active pointer and fetch its position
                    val pointerIndex = e.findPointerIndex(mActivePointerId)

                    val x = e.getX(pointerIndex)
                    val y = e.getY(pointerIndex)

                    if (!isScaling && mScaleFactor > 1) {
                        // Calculate the distance moved
                        val dx = x - mLastTouchX
                        val dy = y - mLastTouchY

                        setTranslateXY(mTranX + dx, mTranY + dy)
                        correctTranslateXY()
                    }

                    invalidate()
                    // Remember this touch position for the next move event
                    mLastTouchX = x
                    mLastTouchY = y

                } catch (exception: Exception) {
                    val x = e.x
                    val y = e.y

                    if (!isScaling && mScaleFactor > 1 && mLastTouchX != INVALID_TOUCH_POSITION) { // 缩放时不做处理
                        // Calculate the distance moved
                        val dx = x - mLastTouchX
                        val dy = y - mLastTouchY

                        setTranslateXY(mTranX + dx, mTranY + dy)
                        correctTranslateXY()
                    }

                    invalidate()
                    // Remember this touch position for the next move event
                    mLastTouchX = x
                    mLastTouchY = y
                }
            }

            ACTION_UP -> {

            }

            ACTION_CANCEL -> {

            }

            ACTION_POINTER_UP -> {
                val pointerIndex = e.getActionIndex()
                val pointerId = e.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mLastTouchX = e.getX(newPointerIndex)
                    mLastTouchY = e.getY(newPointerIndex)
                    mActivePointerId = e.getPointerId(newPointerIndex)
                }
            }
        }

        return super.onTouchEvent(e) || retVal
    }

    override fun dispatchDraw(canvas: Canvas?) {
        if (mTranX > 0 || mTranY > 0) {
            mTranX = 0f
            mTranY = 0f
        } else if (mScaleFactor == mMinScaleFactor) {
            mTranX = 0f
            mTranY = 0f
        }

        canvas!!.save()
        canvas!!.translate(mTranX, mTranY)
        canvas!!.scale(mScaleFactor, mScaleFactor)

        super.dispatchDraw(canvas)
        canvas.restore()
    }

    private fun setTranslateXY(tranX: Float, tranY: Float) {
        mTranX = tranX
        mTranY = tranY
    }

    private fun correctTranslateXY() {
        val correctXY = correctTranslateXY(mTranX, mTranY)
        mTranX = correctXY[0]
        mTranY = correctXY[1]
    }

    private fun correctTranslateXY(x: Float, y: Float): FloatArray {
        var x = x
        var y = y
        if (mScaleFactor <= 1) {
            return floatArrayOf(x, y)
        }

        if (x > 0.0f) {
            x = 0.0f
        } else if (x < mMaxTranX) {
            x = mMaxTranX
        }

        if (y > 0.0f) {
            y = 0.0f
        } else if (y < mMaxTranY) {
            y = mMaxTranY
        }
        return floatArrayOf(x, y)
    }

    private fun zoom(startVal: Float, endVal: Float) {
        if (mScaleAnimator == null) {
            newZoomAnimation()
        }

        if (mScaleAnimator!!.isRunning()) {
            return
        }

        //set Value
        mMaxTranX = mViewWidth - mViewWidth * endVal
        mMaxTranY = mViewHeight - mViewHeight * endVal

        val startTranX = mTranX
        val startTranY = mTranY
        var endTranX = mTranX - (endVal - startVal) * mScaleCenterX
        var endTranY = mTranY - (endVal - startVal) * mScaleCenterY
        val correct = correctTranslateXY(endTranX, endTranY)
        endTranX = correct[0]
        endTranY = correct[1]

        val scaleHolder = PropertyValuesHolder
            .ofFloat(PROPERTY_SCALE, startVal, endVal)
        val tranXHolder = PropertyValuesHolder
            .ofFloat(PROPERTY_TRANX, startTranX, endTranX)
        val tranYHolder = PropertyValuesHolder
            .ofFloat(PROPERTY_TRANY, startTranY, endTranY)
        mScaleAnimator!!.setValues(scaleHolder, tranXHolder, tranYHolder)
        mScaleAnimator!!.setDuration(mScaleDuration.toLong())
        mScaleAnimator!!.start()
    }

    private fun newZoomAnimation() {
        mScaleAnimator = ValueAnimator()
        mScaleAnimator!!.setInterpolator(DecelerateInterpolator())
        mScaleAnimator!!.addUpdateListener(ValueAnimator.AnimatorUpdateListener { animation ->
            //update scaleFactor & tranX & tranY
            mScaleFactor = animation.getAnimatedValue(PROPERTY_SCALE) as Float
            setTranslateXY(
                animation.getAnimatedValue(PROPERTY_TRANX) as Float,
                animation.getAnimatedValue(PROPERTY_TRANY) as Float
            )
            invalidate()
        })

        // set listener to update scale flag
        mScaleAnimator!!.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationStart(animation: Animator) {
                isScaling = true
            }

            override fun onAnimationEnd(animation: Animator) {
                isScaling = false
            }

            override fun onAnimationCancel(animation: Animator) {
                isScaling = false
            }
        })
    }

    inner class ScaleListener: ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            if (mScaleFactor <= mDefaultScaleFactor) {
                mScaleCenterX = -mTranX / (mScaleFactor - 1)
                mScaleCenterY = -mTranY / (mScaleFactor - 1)
                mScaleCenterX = if (mScaleCenterX.isNaN()
                    || mScaleCenterX.isInfinite()) 0f else mScaleCenterX
                mScaleCenterY = if (mScaleCenterY.isNaN()
                    || mScaleCenterY.isInfinite()) 0f else mScaleCenterY
                zoom(mScaleFactor, mDefaultScaleFactor)
            }
            isScaling = false
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            val mLastScale = mScaleFactor
            mScaleFactor *= detector!!.getScaleFactor()

            mScaleFactor = Math.max(mMinScaleFactor, Math.min(mScaleFactor, mMaxScaleFactor))

            mMaxTranX = mViewWidth - mViewWidth * mScaleFactor
            mMaxTranY = mViewHeight - mViewHeight * mScaleFactor

            mScaleCenterX = detector!!.getFocusX()
            mScaleCenterY = detector!!.getFocusY()

            val offsetX = mScaleCenterX * (mLastScale - mScaleFactor)
            val offsetY = mScaleCenterY * (mLastScale - mScaleFactor)

            setTranslateXY(mTranX + offsetX, mTranY + offsetY)

            isScaling = true
            invalidate()
            return true
        }
    }

    inner class GestureListener: GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            val startFactor = mScaleFactor
            val endFactor: Float

            if (mScaleFactor == mDefaultScaleFactor) {
                mScaleCenterX = e!!.getX()
                mScaleCenterY = e!!.getY()
                endFactor = mMaxScaleFactor
            } else {
                mScaleCenterX = if (mScaleFactor == 1f) e!!.getX() else -mTranX / (mScaleFactor - 1)
                mScaleCenterY = if (mScaleFactor == 1f) e!!.getY() else -mTranY / (mScaleFactor - 1)
                endFactor = mDefaultScaleFactor
            }
            zoom(startFactor, endFactor)
            return super.onDoubleTap(e)
        }
    }

    // public method
    fun setEnableScale(enable: Boolean) {
        if (isEnableScale == enable) {
            return
        }
        this.isEnableScale = enable

        if (!isEnableScale && mScaleFactor != 1f) {
            zoom(mScaleFactor, 1f)
        }
    }

    fun isEnableScale(): Boolean {
        return isEnableScale
    }
}