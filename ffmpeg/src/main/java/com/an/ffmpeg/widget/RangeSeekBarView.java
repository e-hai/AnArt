package com.an.ffmpeg.widget;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.an.ffmpeg.R;


/**
 * 实现思路：
 * 1、定义最小选择区域和最大选择区域，最大选择区域是固定位置，而最小区域并不是固定的，因此等同于最小选择宽=(minRangeTime/maxRangeTime)*maxRange
 * 2、已选区域限定在最小和最大之间
 * 3、绘制元素均按照已选区域来调整即可
 * 4、仅需知道最大区域左侧关联的视频的帧，加上maxTime即可计算已选区域两侧对应的帧，播放进度条对应的帧
 **/
public class RangeSeekBarView extends View {

    private static final String TAG = RangeSeekBarView.class.getSimpleName();

    public static final int INVALID_POINTER_ID = 255;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;
    private int activePointerId = INVALID_POINTER_ID;

    private double normalizedMinValueTime = 0d;
    private double normalizedMaxValueTime = 1d;// normalized：规格化的--点坐标占总长度的比例值，范围从0-1

    /**
     * 最大可截取区域-对应UI的内部显示区域，其他元素均需要以此为基准调整位置
     **/
    private Paint maxRangePaint;    //画笔-绘制最大截取区域边框
    private RectF maxRangeRect = new RectF();     //最大可截取区域
    private final int maxRangeMarginStartEnd = Utils.dpToPx(38); //左右与view边界的距离
    private final int maxRangeMarginTopBottom = Utils.dpToPx(4); //上下与view边界的距离
    private final float maxRangeRound = Utils.dpToPx(4);
    private float minRangeWidth = 1;//最小裁剪距离

    /**
     * 已选中的截取区域
     **/
    private Paint selectRangePaint;  //画笔-绘制已选中的截取区域边框
    private RectF selectRangeRect = new RectF();   //已选中的截取区域
    private final int borderSize = Utils.dpToPx(4);

    /**
     * 指示器
     **/
    private Thumb pressedThumb;
    private Paint thumbPaint;        //画笔-绘制指示器
    private Bitmap thumbLeftBitmap;  //左侧指示器位图
    private Bitmap thumbRightBitmap; //右侧指示器位图
    private final int thumbWidth = Utils.dpToPx(18);  //指示器宽
    private final int thumbHeight = Utils.dpToPx(72); //指示器高

    /**
     * 播放进度条-范围在已选区域
     **/
    private float progress = 0; //进度 范围0-1
    private Paint progressPaint;
    private final int progressWidth = Utils.dpToPx(2);
    private final int progressHeight = Utils.dpToPx(68);

    /**
     * 两侧阴影
     **/
    private final Paint shadowPaint = new Paint();      //画笔-绘制两边阴影

    /**
     * 裁剪时间
     **/
    private double selectRangeLeftNormalized = 0d;  //点坐标占总长度的比例值，范围从0-1
    private double selectRangeRightNormalized = 1d; //点坐标占总长度的比例值，范围从0-1
    private final Paint selectRangeLeftTimePaint = new Paint(); //画笔-左侧裁剪的视频时间
    private final Paint selectRangeRightTimePaint = new Paint();//画笔-右侧裁剪的视频时间
    private final int timeTextSize = Utils.spToPx(0);

    private long startTime = 0;  //最大可截取区域左侧对应的视频帧
    private long minRangeTime = 0; //最小可截取时间
    private long maxRangeTime = 0; //最大可截取时间


    private float mDownMotionX;
    private boolean mIsDragging;

    private OnRangeSeekBarChangeListener mRangeSeekBarChangeListener;

    public enum Thumb {
        MIN, MAX
    }

    public RangeSeekBarView(Context context, long minRangeTime, long maxRangeTime) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setMinRangeTime(minRangeTime);
        setMaxRangeTime(maxRangeTime);
        init();
    }


    private void init() {
        final int selectRangeColorRes = getContext().getResources().getColor(R.color.main_color);
        final int timeColorRes = getContext().getResources().getColor(R.color.main_color);
        final int shootMaxRangeColorRes = getContext().getResources().getColor(R.color.shoot_max_range);
        final int shadowColor = getContext().getResources().getColor(R.color.shadow_color);

        //初始化指示器
        thumbLeftBitmap = getScaleBitmap(
                BitmapFactory.decodeResource(getResources(), R.drawable.icon_thumb_left),
                thumbWidth,
                thumbHeight
        );
        thumbRightBitmap = getScaleBitmap(
                BitmapFactory.decodeResource(getResources(), R.drawable.icon_thumb_right),
                thumbWidth,
                thumbHeight
        );
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        //初始化进度条
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.WHITE);

        //初始化最大可选范围
        maxRangePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maxRangePaint.setAntiAlias(true);
        maxRangePaint.setColor(shootMaxRangeColorRes);
        maxRangePaint.setStrokeWidth(borderSize);
        maxRangePaint.setStyle(Paint.Style.STROKE);

        //初始化两侧阴影
        shadowPaint.setAntiAlias(true);
        shadowPaint.setColor(shadowColor);

        //初始化上下边框
        selectRangePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectRangePaint.setAntiAlias(true);
        selectRangePaint.setStyle(Paint.Style.STROKE);
        selectRangePaint.setStrokeWidth(borderSize);
        selectRangePaint.setColor(selectRangeColorRes);

        //左侧裁剪时间
        selectRangeLeftTimePaint.setStrokeWidth(3);
        selectRangeLeftTimePaint.setTextSize(timeTextSize);
        selectRangeLeftTimePaint.setAntiAlias(true);
        selectRangeLeftTimePaint.setColor(timeColorRes);
        selectRangeLeftTimePaint.setTextAlign(Paint.Align.LEFT);

        //右侧裁剪时间
        selectRangeRightTimePaint.setStrokeWidth(3);
        selectRangeRightTimePaint.setTextSize(timeTextSize);
        selectRangeRightTimePaint.setAntiAlias(true);
        selectRangeRightTimePaint.setColor(timeColorRes);
        selectRangeRightTimePaint.setTextAlign(Paint.Align.RIGHT);
    }

    /**
     * 根据指定尺寸，缩放bitmap
     **/
    private Bitmap getScaleBitmap(Bitmap originalBitmap, int toWidth, int toHeight) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        float scaleWidth = toWidth * 1.0f / width;
        float scaleHeight = toHeight * 1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 300;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = 120;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //由于边框绘制时，以边位置为中心，因此需向外扩大边框大小的一半
        float out = -borderSize / 2f;

        maxRangeRect = new RectF(
                maxRangeMarginStartEnd,
                maxRangeMarginTopBottom,
                w - maxRangeMarginStartEnd,
                h - maxRangeMarginTopBottom
        );
        maxRangeRect.inset(out, out);

        minRangeWidth = (minRangeTime * 1f) / (maxRangeTime * 1f) * maxRangeRect.width();
        selectRangeRect = new RectF(maxRangeRect);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawMaxRange(canvas);
        drawShadow(canvas);
        drawSelectRange(canvas);
        drawProgress(canvas);
        drawThumb(canvas);
        drawCutTimeText(canvas);
    }

    private void drawProgress(Canvas canvas) {
        if (progress <= 0) return;
        float left = (getSelectRangeLeftX() + progress * selectRangeRect.width());
        float top = selectRangeRect.top;
        canvas.drawRoundRect(
                left,
                top,
                left + progressWidth,
                progressHeight,
                6,
                6,
                progressPaint);
    }

    private void drawMaxRange(Canvas canvas) {
        canvas.drawRoundRect(maxRangeRect, maxRangeRound, maxRangeRound, maxRangePaint);
    }

    /**
     * 绘制已选区域
     **/
    private void drawSelectRange(Canvas canvas) {
        canvas.drawRect(selectRangeRect, selectRangePaint);
    }

    /**
     * 绘制两侧阴影
     **/
    private void drawShadow(Canvas canvas) {
        //左边阴影范围：由布局左侧到视频左截取处
        int lShadowStart = 0;
        int lShadowEnd = (int) getSelectRangeLeftX();

        //右边阴影范围：由布局右侧到视频右截取处
        int rShadowStart = (int) getSelectRangeRightX();
        int rShadowEnd = getWidth();

        canvas.drawRect(lShadowStart, 0, lShadowEnd, getHeight(), shadowPaint);
        canvas.drawRect(rShadowStart, 0, rShadowEnd, getHeight(), shadowPaint);
    }

    /**
     * 绘制指示器
     **/
    private void drawThumb(Canvas canvas) {
        int lThumbStart = (int) (getSelectRangeLeftX() - thumbWidth);
        int rThumbStart = (int) getSelectRangeRightX();

        canvas.drawBitmap(thumbLeftBitmap, lThumbStart, timeTextSize, thumbPaint);
        canvas.drawBitmap(thumbRightBitmap, rThumbStart, timeTextSize, thumbPaint);
    }

    /**
     * 绘制左右裁剪时间
     **/
    private void drawCutTimeText(Canvas canvas) {
        String leftThumbsTime = Utils.convertSecondsToTime(getSelectedLeftTimeInVideo());
        String rightThumbsTime = Utils.convertSecondsToTime(getSelectedLeftTimeInVideo());
        canvas.drawText(leftThumbsTime, getSelectRangeLeftX(), timeTextSize, selectRangeLeftTimePaint);
        canvas.drawText(rightThumbsTime, getSelectRangeRightX(), timeTextSize, selectRangeRightTimePaint);
    }

    /**
     * 获取左侧裁剪在View的位置
     **/
    private float getSelectRangeLeftX() {
        return selectRangeRect.left;
    }

    /**
     * 获取右侧裁剪的位置
     **/
    private float getSelectRangeRightX() {
        return selectRangeRect.right;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnabled()) return false;

        if (event.getPointerCount() > 1) {
            return super.onTouchEvent(event);
        }

        // 记录点击点的index
        int pointerIndex;
        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //记住最后一个手指点击屏幕的点的坐标x，mDownMotionX
                activePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(activePointerId);
                mDownMotionX = event.getX(pointerIndex);
                // 判断touch到的是最大值thumb还是最小值thumb
                pressedThumb = evalPressedThumb(mDownMotionX);
                if (pressedThumb == null) {
                    return super.onTouchEvent(event);
                }
                onStartDrag();
                //告诉父布局不进行拦截
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarChanged(
                            getSelectedLeftTimeInVideo(),
                            getSelectedRightTimeInVideo(),
                            MotionEvent.ACTION_DOWN,
                            pressedThumb
                    );
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null && mIsDragging) {
                    trackTouchEvent(event);
                    if (mRangeSeekBarChangeListener != null) {
                        mRangeSeekBarChangeListener.onRangeSeekBarChanged(
                                getSelectedLeftTimeInVideo(),
                                getSelectedRightTimeInVideo(),
                                MotionEvent.ACTION_MOVE,
                                pressedThumb
                        );
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                onStopDrag();
                invalidate();
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarChanged(
                            getSelectedLeftTimeInVideo(),
                            getSelectedRightTimeInVideo(),
                            MotionEvent.ACTION_UP,
                            pressedThumb
                    );
                }
                pressedThumb = null;// 手指抬起，则置被touch到的thumb为空
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = event.getPointerCount() - 1;
                mDownMotionX = event.getX(index);
                activePointerId = event.getPointerId(index);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                onStopDrag();
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == activePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            activePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    /**
     * 跟踪触摸事件，调整选择的裁剪区域
     **/
    private void trackTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) return;
        // 得到按下点的index，获取触摸在View上的x坐标
        final int pointerIndex = event.findPointerIndex(activePointerId);
        float x;
        try {
            x = event.getX(pointerIndex);
        } catch (Exception e) {
            return;
        }
        if (Thumb.MIN.equals(pressedThumb)) {
            //1：已选左侧不能超出最大可选择区域左侧
            //2: 选择区域间距离不能超过最小范围
            float minSelectRangeLeft = maxRangeRect.left;
            float maxSelectRangeLeft = selectRangeRect.right - minRangeWidth;
            float newSelectRangeLeft = x;
            if (newSelectRangeLeft < minSelectRangeLeft) {
                newSelectRangeLeft = minSelectRangeLeft;
            } else if (newSelectRangeLeft > maxSelectRangeLeft) {
                newSelectRangeLeft = maxSelectRangeLeft;
            }
            selectRangeRect.left = newSelectRangeLeft;

        } else if (Thumb.MAX.equals(pressedThumb)) {

            //1：已选右侧不能超出最大可选择区域右侧
            //2: 选择区域间距离不能超过最小范围
            float minSelectRangeRight = selectRangeRect.left + minRangeWidth;
            float maxSelectRangeRight = maxRangeRect.right;
            float newSelectRangeRight = x;
            if (newSelectRangeRight < minSelectRangeRight) {
                newSelectRangeRight = minSelectRangeRight;
            } else if (newSelectRangeRight > maxSelectRangeRight) {
                newSelectRangeRight = maxSelectRangeRight;
            }
            selectRangeRect.right = newSelectRangeRight;
        }
        invalidate();
    }


    /**
     * 计算位于哪个Thumb内
     *
     * @param touchX touchX
     * @return 被touch的是空还是最大值或最小值
     */
    private Thumb evalPressedThumb(float touchX) {
        Thumb result = null;
        boolean minThumbPressed = isInThumbLeft(touchX);
        boolean maxThumbPressed = isInThumbRight(touchX);
        if (minThumbPressed && maxThumbPressed) {
            // 如果两个thumbs重叠在一起，无法判断拖动哪个，做以下处理
            // 触摸点在屏幕右侧，则判断为touch到了最小值thumb，反之判断为touch到了最大值thumb
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        } else if (minThumbPressed) {
            result = Thumb.MIN;
        } else if (maxThumbPressed) {
            result = Thumb.MAX;
        }
        return result;
    }

    /**
     * 触摸点在已选区域右侧和右指示器右侧之间
     **/
    private boolean isInThumbRight(float touchX) {
        return selectRangeRect.right < touchX && touchX < (selectRangeRect.right + thumbWidth);
    }

    /**
     * 触摸点在已选区域左侧和右指示器左侧之间
     **/
    private boolean isInThumbLeft(float touchX) {
        return (selectRangeRect.left - thumbWidth) < touchX && touchX < selectRangeRect.left;
    }


    private void onStartDrag() {
        mIsDragging = true;
        setPressed(true);
        resetProgress();
    }

    private void onStopDrag() {
        mIsDragging = false;
        setPressed(false);
    }

    private void resetProgress() {
        progress = 0;
    }

    /**
     * 指视频时间
     *
     * @param minRangeTime 秒
     **/
    public void setMinRangeTime(long minRangeTime) {
        this.minRangeTime = minRangeTime;
        Log.d(TAG, "minRangeTime=" + minRangeTime);
    }

    /**
     * 指视频时间
     *
     * @param maxRangeTime 秒
     **/
    public void setMaxRangeTime(long maxRangeTime) {
        this.maxRangeTime = maxRangeTime;
        Log.d(TAG, "maxRangeTime=" + maxRangeTime);
    }

    /**
     * 指视频时间
     *
     * @param time 最大可选区域左侧对应的时间 秒
     **/
    void setStartTimeInVideo(long time) {
        startTime = time;
        resetProgress();
        if (mRangeSeekBarChangeListener != null) {
            mRangeSeekBarChangeListener.onRangeSeekBarChanged(
                    getSelectedLeftTimeInVideo(),
                    getSelectedRightTimeInVideo(),
                    MotionEvent.ACTION_MOVE,
                    Thumb.MIN
            );
        }
    }


    /**
     * 指视频时间
     * 已选区域所对应的视频时间 秒
     **/
    int getSelectedLeftTimeInVideo() {
        return (int) (startTime + (selectRangeRect.left / maxRangeRect.width()) * maxRangeTime);
    }

    /**
     * 指视频时间
     * 已选区域所对应的视频时间 秒
     **/
    int getSelectedRightTimeInVideo() {
        return (int) (startTime + (selectRangeRect.right / maxRangeRect.width()) * maxRangeTime);
    }

    /**
     * 指视频时间
     * 已选多少秒时间
     **/
    long getSelectTime() {
        return getSelectedRightTimeInVideo() - getSelectedLeftTimeInVideo();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", selectRangeLeftNormalized);
        bundle.putDouble("MAX", selectRangeRightNormalized);
        bundle.putDouble("MIN_TIME", normalizedMinValueTime);
        bundle.putDouble("MAX_TIME", normalizedMaxValueTime);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        selectRangeLeftNormalized = bundle.getDouble("MIN");
        selectRangeRightNormalized = bundle.getDouble("MAX");
        normalizedMinValueTime = bundle.getDouble("MIN_TIME");
        normalizedMaxValueTime = bundle.getDouble("MAX_TIME");
    }

    public interface OnRangeSeekBarChangeListener {
        void onRangeSeekBarChanged(long leftSelectTime, long rightSelectTime, int action, Thumb pressedThumb);
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        this.mRangeSeekBarChangeListener = listener;
    }

    private ValueAnimator progressAnimator;

    void playingProgressAnimation() {
        pauseProgressAnimation();
        playingAnimation();
    }

    void pauseProgressAnimation() {
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }
    }


    private void playingAnimation() {
        float start = progress;
        float end = 1;
        long duration = (long) ((getSelectTime() - getSelectTime() * progress) * 1000); //毫秒
        Log.d(TAG, "start=" + start + " duration=" + duration);
        progressAnimator = ValueAnimator.ofFloat(start, end).setDuration(duration);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            Log.d(TAG, "progress=" + progress);
            invalidate();
        });
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetProgress();
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarChanged(
                            getSelectedLeftTimeInVideo(),
                            getSelectedRightTimeInVideo(),
                            MotionEvent.ACTION_CANCEL,
                            Thumb.MIN
                    );
                }
                Log.d(TAG, "onAnimationEnd=" + progress);
            }
        });
        progressAnimator.start();
    }

}
