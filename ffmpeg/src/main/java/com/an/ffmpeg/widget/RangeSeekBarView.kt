package com.an.ffmpeg.widget

import android.animation.Animator
import android.content.Context
import com.an.ffmpeg.R
import android.annotation.SuppressLint
import android.view.MotionEvent
import java.lang.Exception
import android.os.Parcelable
import android.os.Bundle
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.AnimatorListenerAdapter
import android.graphics.*
import android.view.View
import com.an.ffmpeg.code.Utils

/**
 * 实现思路：
 * 1、定义最小选择区域和最大选择区域，最大选择区域是固定位置，而最小区域并不是固定的，因此等同于最小选择宽=(minRangeTime/maxRangeTime)*maxRange
 * 2、已选区域限定在最小和最大之间
 * 3、绘制元素均按照已选区域来调整即可
 * 4、仅需知道最大区域左侧关联的视频的帧，加上maxTime即可计算已选区域两侧对应的帧，播放进度条对应的帧
 */
class RangeSeekBarView(context: Context, minRangeTime: Long, maxRangeTime: Long) : View(context) {

    private var activePointerId = INVALID_POINTER_ID
    private var normalizedMinValueTime = 0.0
    private var normalizedMaxValueTime = 1.0 // normalized：规格化的--点坐标占总长度的比例值，范围从0-1

    /**
     * 最大可截取区域-对应UI的内部显示区域，其他元素均需要以此为基准调整位置
     */
    private var maxRangePaint: Paint//画笔-绘制最大截取区域边框
    private var maxRangeRect = RectF() //最大可截取区域
    private val maxRangeMarginStartEnd = Utils.dpToPx(context, 38) //左右与view边界的距离
    private val maxRangeMarginTopBottom = Utils.dpToPx(context, 4) //上下与view边界的距离
    private val maxRangeRound = Utils.dpToPx(context, 4).toFloat()
    private var minRangeWidth = 1f //最小裁剪距离

    /**
     * 已选中的截取区域
     */
    private var selectRangePaint: Paint//画笔-绘制已选中的截取区域边框
    private var selectRangeRect = RectF() //已选中的截取区域
    private val borderSize = Utils.dpToPx(context,4)

    /**
     * 指示器
     */
    private var pressedThumb: Thumb? = null
    private var thumbPaint: Paint//画笔-绘制指示器
    private var thumbLeftBitmap: Bitmap//左侧指示器位图
    private var thumbRightBitmap: Bitmap//右侧指示器位图
    private val thumbWidth = Utils.dpToPx(context,18) //指示器宽
    private val thumbHeight = Utils.dpToPx(context,72) //指示器高

    /**
     * 播放进度条-范围在已选区域
     */
    private var progress = 0f //进度 范围0-1
    private var progressPaint: Paint
    private val progressWidth = Utils.dpToPx(context,2)
    private val progressHeight = Utils.dpToPx(context,68)
    private var progressAnimator: ValueAnimator? = null

    /**
     * 两侧阴影
     */
    private val shadowPaint = Paint() //画笔-绘制两边阴影

    /**
     * 裁剪时间
     */
    private var selectRangeLeftNormalized = 0.0 //点坐标占总长度的比例值，范围从0-1
    private var selectRangeRightNormalized = 1.0 //点坐标占总长度的比例值，范围从0-1
    private val selectRangeLeftTimePaint = Paint() //画笔-左侧裁剪的视频时间
    private val selectRangeRightTimePaint = Paint() //画笔-右侧裁剪的视频时间
    private val timeTextSize = Utils.spToPx(context,0)
    private var startTime: Long = 0 //最大可截取区域左侧对应的视频帧
    private var minRangeTime: Long = 0 //最小可截取时间
    private var maxRangeTime: Long = 0 //最大可截取时间
    private var mDownMotionX = 0f
    private var mIsDragging = false
    private var mRangeSeekBarChangeListener: OnRangeSeekBarChangeListener? = null

    enum class Thumb {
        MIN, MAX
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setMinRangeTime(minRangeTime)
        setMaxRangeTime(maxRangeTime)
        val selectRangeColorRes = context.resources.getColor(R.color.main_color)
        val timeColorRes = context.resources.getColor(R.color.main_color)
        val shootMaxRangeColorRes = context.resources.getColor(R.color.shoot_max_range)
        val shadowColor = context.resources.getColor(R.color.shadow_color)

        //初始化指示器
        thumbLeftBitmap = getScaleBitmap(
            BitmapFactory.decodeResource(resources, R.drawable.icon_thumb_left),
            thumbWidth,
            thumbHeight
        )
        thumbRightBitmap = getScaleBitmap(
            BitmapFactory.decodeResource(resources, R.drawable.icon_thumb_right),
            thumbWidth,
            thumbHeight
        )
        thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        //初始化进度条
        progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressPaint.color = Color.WHITE

        //初始化最大可选范围
        maxRangePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maxRangePaint.isAntiAlias = true
        maxRangePaint.color = shootMaxRangeColorRes
        maxRangePaint.strokeWidth = borderSize.toFloat()
        maxRangePaint.style = Paint.Style.STROKE

        //初始化两侧阴影
        shadowPaint.isAntiAlias = true
        shadowPaint.color = shadowColor

        //初始化上下边框
        selectRangePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        selectRangePaint.isAntiAlias = true
        selectRangePaint.style = Paint.Style.STROKE
        selectRangePaint.strokeWidth = borderSize.toFloat()
        selectRangePaint.color = selectRangeColorRes

        //左侧裁剪时间
        selectRangeLeftTimePaint.strokeWidth = 3f
        selectRangeLeftTimePaint.textSize = timeTextSize.toFloat()
        selectRangeLeftTimePaint.isAntiAlias = true
        selectRangeLeftTimePaint.color = timeColorRes
        selectRangeLeftTimePaint.textAlign = Paint.Align.LEFT

        //右侧裁剪时间
        selectRangeRightTimePaint.strokeWidth = 3f
        selectRangeRightTimePaint.textSize = timeTextSize.toFloat()
        selectRangeRightTimePaint.isAntiAlias = true
        selectRangeRightTimePaint.color = timeColorRes
        selectRangeRightTimePaint.textAlign = Paint.Align.RIGHT
    }

    /**
     * 根据指定尺寸，缩放bitmap
     */
    private fun getScaleBitmap(originalBitmap: Bitmap, toWidth: Int, toHeight: Int): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaleWidth = toWidth * 1.0f / width
        val scaleHeight = toHeight * 1.0f / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 300
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec)
        }
        var height = 120
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //由于边框绘制时，以边位置为中心，因此需向外扩大边框大小的一半
        val out = -borderSize / 2f
        maxRangeRect = RectF(
            maxRangeMarginStartEnd.toFloat(),
            maxRangeMarginTopBottom.toFloat(),
            (w - maxRangeMarginStartEnd).toFloat(),
            (h - maxRangeMarginTopBottom).toFloat()
        )
        maxRangeRect.inset(out, out)
        minRangeWidth = minRangeTime * 1f / (maxRangeTime * 1f) * maxRangeRect.width()
        selectRangeRect = RectF(maxRangeRect)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawMaxRange(canvas)
        drawShadow(canvas)
        drawSelectRange(canvas)
        drawProgress(canvas)
        drawThumb(canvas)
        drawCutTimeText(canvas)
    }

    private fun drawProgress(canvas: Canvas) {
        if (progress <= 0) return
        val left = selectRangeLeftX + progress * selectRangeRect.width()
        val top = selectRangeRect.top
        canvas.drawRoundRect(
            left,
            top,
            left + progressWidth,
            progressHeight.toFloat(), 6f, 6f,
            progressPaint
        )
    }

    private fun drawMaxRange(canvas: Canvas) {
        canvas.drawRoundRect(maxRangeRect, maxRangeRound, maxRangeRound, maxRangePaint)
    }

    /**
     * 绘制已选区域
     */
    private fun drawSelectRange(canvas: Canvas) {
        canvas.drawRect(selectRangeRect, selectRangePaint)
    }

    /**
     * 绘制两侧阴影
     */
    private fun drawShadow(canvas: Canvas) {
        //左边阴影范围：由布局左侧到视频左截取处
        val lShadowStart = 0
        val lShadowEnd = selectRangeLeftX.toInt()

        //右边阴影范围：由布局右侧到视频右截取处
        val rShadowStart = selectRangeRightX.toInt()
        val rShadowEnd = width
        canvas.drawRect(
            lShadowStart.toFloat(),
            0f,
            lShadowEnd.toFloat(),
            height.toFloat(),
            shadowPaint
        )
        canvas.drawRect(
            rShadowStart.toFloat(),
            0f,
            rShadowEnd.toFloat(),
            height.toFloat(),
            shadowPaint
        )
    }

    /**
     * 绘制指示器
     */
    private fun drawThumb(canvas: Canvas) {
        val lThumbStart = (selectRangeLeftX - thumbWidth).toInt()
        val rThumbStart = selectRangeRightX.toInt()
        canvas.drawBitmap(
            thumbLeftBitmap,
            lThumbStart.toFloat(),
            timeTextSize.toFloat(),
            thumbPaint
        )
        canvas.drawBitmap(
            thumbRightBitmap,
            rThumbStart.toFloat(),
            timeTextSize.toFloat(),
            thumbPaint
        )
    }

    /**
     * 绘制左右裁剪时间
     */
    private fun drawCutTimeText(canvas: Canvas) {
        val leftThumbsTime = Utils.convertSecondsToTime(
            selectedLeftTimeInVideo.toLong()
        )
        val rightThumbsTime = Utils.convertSecondsToTime(
            selectedLeftTimeInVideo.toLong()
        )
        canvas.drawText(
            leftThumbsTime,
            selectRangeLeftX,
            timeTextSize.toFloat(),
            selectRangeLeftTimePaint
        )
        canvas.drawText(
            rightThumbsTime,
            selectRangeRightX,
            timeTextSize.toFloat(),
            selectRangeRightTimePaint
        )
    }

    /**
     * 获取左侧裁剪在View的位置
     */
    private val selectRangeLeftX: Float
        get() = selectRangeRect.left

    /**
     * 获取右侧裁剪的位置
     */
    private val selectRangeRightX: Float
        get() = selectRangeRect.right

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        if (event.pointerCount > 1) {
            return super.onTouchEvent(event)
        }

        // 记录点击点的index
        val pointerIndex: Int
        val action = event.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //记住最后一个手指点击屏幕的点的坐标x，mDownMotionX
                activePointerId = event.getPointerId(event.pointerCount - 1)
                pointerIndex = event.findPointerIndex(activePointerId)
                mDownMotionX = event.getX(pointerIndex)
                // 判断touch到的是最大值thumb还是最小值thumb
                pressedThumb = evalPressedThumb(mDownMotionX)
                if (pressedThumb == null) {
                    return super.onTouchEvent(event)
                }
                onStartDrag()
                //告诉父布局不进行拦截
                parent?.requestDisallowInterceptTouchEvent(true)
                mRangeSeekBarChangeListener?.onRangeSeekBarChanged(
                    selectedLeftTimeInVideo.toLong(),
                    selectedRightTimeInVideo.toLong(),
                    MotionEvent.ACTION_DOWN,
                    pressedThumb
                )
            }
            MotionEvent.ACTION_MOVE -> if (pressedThumb != null && mIsDragging) {
                trackTouchEvent(event)
                mRangeSeekBarChangeListener?.onRangeSeekBarChanged(
                    selectedLeftTimeInVideo.toLong(),
                    selectedRightTimeInVideo.toLong(),
                    MotionEvent.ACTION_MOVE,
                    pressedThumb
                )
            }
            MotionEvent.ACTION_UP -> {
                onStopDrag()
                invalidate()
                mRangeSeekBarChangeListener?.onRangeSeekBarChanged(
                    selectedLeftTimeInVideo.toLong(),
                    selectedRightTimeInVideo.toLong(),
                    MotionEvent.ACTION_UP,
                    pressedThumb
                )
                pressedThumb = null // 手指抬起，则置被touch到的thumb为空
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.pointerCount - 1
                mDownMotionX = event.getX(index)
                activePointerId = event.getPointerId(index)
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(event)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                onStopDrag()
                invalidate()
            }
            else -> {}
        }
        return true
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.action and ACTION_POINTER_INDEX_MASK shr ACTION_POINTER_INDEX_SHIFT
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == activePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mDownMotionX = ev.getX(newPointerIndex)
            activePointerId = ev.getPointerId(newPointerIndex)
        }
    }

    /**
     * 跟踪触摸事件，调整选择的裁剪区域
     */
    private fun trackTouchEvent(event: MotionEvent) {
        if (event.pointerCount > 1) return
        // 得到按下点的index，获取触摸在View上的x坐标
        val pointerIndex = event.findPointerIndex(activePointerId)
        val x: Float = try {
            event.getX(pointerIndex)
        } catch (e: Exception) {
            return
        }
        if (Thumb.MIN == pressedThumb) {
            //1：已选左侧不能超出最大可选择区域左侧
            //2: 选择区域间距离不能超过最小范围
            val minSelectRangeLeft = maxRangeRect.left
            val maxSelectRangeLeft = selectRangeRect.right - minRangeWidth
            var newSelectRangeLeft = x
            if (newSelectRangeLeft < minSelectRangeLeft) {
                newSelectRangeLeft = minSelectRangeLeft
            } else if (newSelectRangeLeft > maxSelectRangeLeft) {
                newSelectRangeLeft = maxSelectRangeLeft
            }
            selectRangeRect.left = newSelectRangeLeft
        } else if (Thumb.MAX == pressedThumb) {

            //1：已选右侧不能超出最大可选择区域右侧
            //2: 选择区域间距离不能超过最小范围
            val minSelectRangeRight = selectRangeRect.left + minRangeWidth
            val maxSelectRangeRight = maxRangeRect.right
            var newSelectRangeRight = x
            if (newSelectRangeRight < minSelectRangeRight) {
                newSelectRangeRight = minSelectRangeRight
            } else if (newSelectRangeRight > maxSelectRangeRight) {
                newSelectRangeRight = maxSelectRangeRight
            }
            selectRangeRect.right = newSelectRangeRight
        }
        invalidate()
    }

    /**
     * 计算位于哪个Thumb内
     *
     * @param touchX touchX
     * @return 被touch的是空还是最大值或最小值
     */
    private fun evalPressedThumb(touchX: Float): Thumb? {
        var result: Thumb? = null
        val minThumbPressed = isInThumbLeft(touchX)
        val maxThumbPressed = isInThumbRight(touchX)
        if (minThumbPressed && maxThumbPressed) {
            // 如果两个thumbs重叠在一起，无法判断拖动哪个，做以下处理
            // 触摸点在屏幕右侧，则判断为touch到了最小值thumb，反之判断为touch到了最大值thumb
            result = if (touchX / width > 0.5f) Thumb.MIN else Thumb.MAX
        } else if (minThumbPressed) {
            result = Thumb.MIN
        } else if (maxThumbPressed) {
            result = Thumb.MAX
        }
        return result
    }

    /**
     * 触摸点在已选区域右侧和右指示器右侧之间
     */
    private fun isInThumbRight(touchX: Float): Boolean {
        return selectRangeRect.right < touchX && touchX < selectRangeRect.right + thumbWidth
    }

    /**
     * 触摸点在已选区域左侧和右指示器左侧之间
     */
    private fun isInThumbLeft(touchX: Float): Boolean {
        return selectRangeRect.left - thumbWidth < touchX && touchX < selectRangeRect.left
    }

    private fun onStartDrag() {
        mIsDragging = true
        isPressed = true
        resetProgress()
    }

    private fun onStopDrag() {
        mIsDragging = false
        isPressed = false
    }

    private fun resetProgress() {
        progress = 0f
    }

    /**
     * 指视频时间
     *
     * @param minRangeTime 秒
     */
    private fun setMinRangeTime(minRangeTime: Long) {
        this.minRangeTime = minRangeTime
    }

    /**
     * 指视频时间
     *
     * @param maxRangeTime 秒
     */
    private fun setMaxRangeTime(maxRangeTime: Long) {
        this.maxRangeTime = maxRangeTime
    }

    /**
     * 指视频时间
     *
     * @param time 最大可选区域左侧对应的时间 秒
     */
    fun setStartTimeInVideo(time: Long) {
        startTime = time
        resetProgress()
        mRangeSeekBarChangeListener?.onRangeSeekBarChanged(
            selectedLeftTimeInVideo.toLong(),
            selectedRightTimeInVideo.toLong(),
            MotionEvent.ACTION_MOVE,
            Thumb.MIN
        )
    }

    /**
     * 指视频时间
     * 已选区域所对应的视频时间 秒
     */
    val selectedLeftTimeInVideo: Int
        get() = (startTime + selectRangeRect.left / maxRangeRect.width() * maxRangeTime).toInt()

    /**
     * 指视频时间
     * 已选区域所对应的视频时间 秒
     */
    val selectedRightTimeInVideo: Int
        get() = (startTime + selectRangeRect.right / maxRangeRect.width() * maxRangeTime).toInt()

    /**
     * 指视频时间
     * 已选多少秒时间
     */
    val selectTime: Long
        get() = (selectedRightTimeInVideo - selectedLeftTimeInVideo).toLong()

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("SUPER", super.onSaveInstanceState())
        bundle.putDouble("MIN", selectRangeLeftNormalized)
        bundle.putDouble("MAX", selectRangeRightNormalized)
        bundle.putDouble("MIN_TIME", normalizedMinValueTime)
        bundle.putDouble("MAX_TIME", normalizedMaxValueTime)
        return bundle
    }

    override fun onRestoreInstanceState(parcel: Parcelable) {
        val bundle = parcel as Bundle
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"))
        selectRangeLeftNormalized = bundle.getDouble("MIN")
        selectRangeRightNormalized = bundle.getDouble("MAX")
        normalizedMinValueTime = bundle.getDouble("MIN_TIME")
        normalizedMaxValueTime = bundle.getDouble("MAX_TIME")
    }

    interface OnRangeSeekBarChangeListener {
        fun onRangeSeekBarChanged(
            leftSelectTime: Long,
            rightSelectTime: Long,
            action: Int,
            pressedThumb: Thumb?
        )
    }

    fun setOnRangeSeekBarChangeListener(listener: OnRangeSeekBarChangeListener?) {
        mRangeSeekBarChangeListener = listener
    }

    fun playingProgressAnimation() {
        pauseProgressAnimation()
        playingAnimation()
    }

    fun pauseProgressAnimation() {
        if (progressAnimator != null && progressAnimator?.isRunning == false) {
            progressAnimator?.cancel()
        }
    }

    private fun playingAnimation() {
        val start = progress
        val end = 1f
        val durationMs = ((selectTime - selectTime * progress) * 1000).toLong() //毫秒
        progressAnimator = ValueAnimator.ofFloat(start, end).setDuration(durationMs).apply {
            interpolator = LinearInterpolator()
            addUpdateListener { animation: ValueAnimator ->
                progress = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    resetProgress()
                    mRangeSeekBarChangeListener?.onRangeSeekBarChanged(
                        selectedLeftTimeInVideo.toLong(),
                        selectedRightTimeInVideo.toLong(),
                        MotionEvent.ACTION_CANCEL,
                        Thumb.MIN
                    )
                }
            })
            start()
        }

    }

    companion object {
        private val TAG = RangeSeekBarView::class.java.simpleName
        const val INVALID_POINTER_ID = 255
        const val ACTION_POINTER_INDEX_MASK = 0x0000ff00
        const val ACTION_POINTER_INDEX_SHIFT = 8
    }


}