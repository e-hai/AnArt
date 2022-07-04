package com.an.ffmpeg.widget;

import static com.an.ffmpeg.widget.VideoTrimmerUtil.MAX_COUNT_RANGE;
import static com.an.ffmpeg.widget.VideoTrimmerUtil.MAX_SHOOT_DURATION_SECONDS;
import static com.an.ffmpeg.widget.VideoTrimmerUtil.MIN_SHOOT_DURATION_SECONDS;
import static com.an.ffmpeg.widget.VideoTrimmerUtil.RECYCLER_VIEW_PADDING;
import static com.an.ffmpeg.widget.VideoTrimmerUtil.THUMBNAIL_SIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.an.ffmpeg.R;

public class VideoTrimmerView extends FrameLayout {

    private static final String TAG = VideoTrimmerView.class.getSimpleName();

    private Context mContext;
    private RelativeLayout mLinearVideo;
    private VideoView mVideoView;
    private ImageView mPlayView;
    private RecyclerView mVideoThumbRecyclerView;
    private RangeSeekBarView mRangeSeekBarView; //裁剪选择框
    private LinearLayout mSeekBarLayout;
    private TextView selectTimeView;
    private Uri mSourceUri;
    private int mDuration = 0; //视频总时长
    private VideoTrimListener mOnTrimVideoListener;
    private VideoTrimmerAdapter mVideoThumbAdapter;
    private int lastScrollX;
    private int mThumbsTotalCount;

    private final int scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

    public VideoTrimmerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoTrimmerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        LayoutInflater.from(context).inflate(R.layout.video_trimmer_view, this, true);

        mLinearVideo = findViewById(R.id.layout_surface_view);
        mVideoView = findViewById(R.id.video_loader);
        mPlayView = findViewById(R.id.icon_video_play);
        mSeekBarLayout = findViewById(R.id.seekBarLayout);
        selectTimeView = findViewById(R.id.select_time_view);
        mVideoThumbRecyclerView = findViewById(R.id.video_frames_recyclerView);
        mVideoThumbRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mVideoThumbAdapter = new VideoTrimmerAdapter();
        mVideoThumbRecyclerView.setAdapter(mVideoThumbAdapter);
        mVideoThumbRecyclerView.addOnScrollListener(mOnScrollListener);
        initListeners();
    }

    private void initListeners() {
        findViewById(R.id.cancelBtn).setOnClickListener(view -> onCancelClicked());
        findViewById(R.id.finishBtn).setOnClickListener(view -> onSaveClicked());
        mVideoView.setOnPreparedListener(mp -> {
            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            videoPrepared(mp);
        });
        mVideoView.setOnCompletionListener(mp -> videoCompleted());
        mPlayView.setOnClickListener(v -> playVideoOrPause());
    }

    /**
     * 初始化裁剪帧
     **/
    private void initRangeSeekBarView(int durationSeconds) {
        if (mRangeSeekBarView != null) return;
        //最大可裁剪时间：当视频时长少于默认值时，最大可裁剪时间即为视频时长
        //由于不需要把所有帧取出来浏览，因此做个规则提取一部分帧
        long maxShootDuration;
        if (durationSeconds <= MAX_SHOOT_DURATION_SECONDS) {
            mThumbsTotalCount = MAX_COUNT_RANGE;
            maxShootDuration = durationSeconds;
        } else {
            mThumbsTotalCount = (int) (durationSeconds * 1.0f / (MAX_SHOOT_DURATION_SECONDS * 1.0f) * MAX_COUNT_RANGE);
            maxShootDuration = MAX_SHOOT_DURATION_SECONDS;
        }
        mVideoThumbRecyclerView.addItemDecoration(new SpacesItemDecoration(RECYCLER_VIEW_PADDING, mThumbsTotalCount));
        mRangeSeekBarView = new RangeSeekBarView(mContext, MIN_SHOOT_DURATION_SECONDS, maxShootDuration);
        mRangeSeekBarView.setStartTimeInVideo(0);
        mRangeSeekBarView.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        mSeekBarLayout.addView(mRangeSeekBarView);
    }

    public void initVideoByURI(final Uri videoURI) {
        mSourceUri = videoURI;
        mVideoView.setVideoURI(videoURI);
        mVideoView.requestFocus();
    }

    private void onCancelClicked() {
        mOnTrimVideoListener.onCancel();
    }

    private void videoPrepared(MediaPlayer mp) {
        ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();

        float videoProportion = (float) videoWidth / (float) videoHeight;
        int screenWidth = mLinearVideo.getWidth();
        int screenHeight = mLinearVideo.getHeight();

        lp.width = screenWidth;
        if (videoHeight > videoWidth) {
            lp.height = screenHeight;
        } else {
            float r = videoHeight / (float) videoWidth;
            lp.height = (int) (lp.width * r);
        }
        mVideoView.setLayoutParams(lp);
        mDuration = mVideoView.getDuration() / 1000;
        initRangeSeekBarView(mDuration);
        startShootVideoThumbs(mContext, mSourceUri, mThumbsTotalCount, 0, mDuration);
    }

    /**
     * 获取预览帧
     **/
    private void startShootVideoThumbs(final Context context, final Uri videoUri, int totalThumbsCount, long startPosition, long endPosition) {
        VideoTrimmerUtil.shootVideoThumbInBackground(context, videoUri, totalThumbsCount, startPosition, endPosition,
                (bitmap, interval) -> {
                    if (bitmap != null) {
                        UiThreadExecutor.runTask("", () -> mVideoThumbAdapter.addBitmaps(bitmap), 0L);
                    }
                });
    }

    private void videoCompleted() {
        setPlayPauseViewIcon(false);
    }


    private void playVideoOrPause() {
        int videoCurrentPosition = mVideoView.getCurrentPosition();
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            mRangeSeekBarView.pauseProgressAnimation();
        } else {
            mVideoView.start();
            mRangeSeekBarView.playingProgressAnimation();
        }
        setPlayPauseViewIcon(mVideoView.isPlaying());
    }


    public void setOnTrimVideoListener(VideoTrimListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }


    private void onSaveClicked() {
        if (mRangeSeekBarView.getSelectedRightTimeInVideo() - mRangeSeekBarView.getSelectedLeftTimeInVideo() < MIN_SHOOT_DURATION_SECONDS) {
            Toast.makeText(mContext, getResources().getString(R.string.video_shoot_min_tip), Toast.LENGTH_SHORT).show();
        } else {
            mVideoView.pause();
            VideoTrimmerUtil.trim(mContext,
                    mSourceUri.getPath(),
                    Utils.getCacheDir(),
                    mRangeSeekBarView.getSelectedLeftTimeInVideo() * 1000,
                    mRangeSeekBarView.getSelectedRightTimeInVideo() * 1000,
                    mOnTrimVideoListener);
        }
    }

    /**
     * @param msec milliseconds
     **/
    private void seekTo(int msec) {
        Log.d(TAG, "seekTo = " + msec);
        mVideoView.seekTo(msec);
    }

    private void setPlayPauseViewIcon(boolean isPlaying) {
        mPlayView.setImageResource(isPlaying ? R.drawable.pause : R.drawable.play);
    }

    private final RangeSeekBarView.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = (leftSelectTime, rightSelectTime, action, pressedThumb) -> {
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                long position;
                if (pressedThumb == RangeSeekBarView.Thumb.MIN) {
                    position = leftSelectTime;
                } else {
                    position = rightSelectTime;
                }
                seekTo((int) position);
                updateSelectTime(leftSelectTime, rightSelectTime);
                break;
            case MotionEvent.ACTION_UP:
                seekTo((int) leftSelectTime);
                break;
            default:
                break;
        }
    };

    @SuppressLint("SetTextI18n")
    private void updateSelectTime(long leftSelectTime, long rightSelectTime) {
        selectTimeView.setText(
                Utils.convertSecondsToTime(leftSelectTime)
                        + "/"
                        + Utils.convertSecondsToTime(rightSelectTime));
    }

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            int scrollX = calcScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < scaledTouchSlop) {
                return;
            }
            //一个预览帧占的时长
            float itemThumbnailTime = (mDuration * 1f) / (mThumbsTotalCount * 1f);
            //一像素占的时长
            float averagePxMs = itemThumbnailTime * 1000 / THUMBNAIL_SIZE;
            //移动的像素占的时长
            long scrollTimeSecond = (long) (scrollX * averagePxMs) / 1000;
            mRangeSeekBarView.setStartTimeInVideo(scrollTimeSecond);
            lastScrollX = scrollX;
        }
    };

    /**
     * 水平滑动了多少px
     */
    private int calcScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mVideoThumbRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    /**
     * Cancel trim thread execut action when finish
     */
    public void onDestroy() {

    }
}
