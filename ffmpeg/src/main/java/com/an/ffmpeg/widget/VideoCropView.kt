package com.an.ffmpeg.widget;

import static com.an.ffmpeg.widget.VideoTrimmerUtil.MAX_COUNT_RANGE;
import static com.an.ffmpeg.widget.VideoTrimmerUtil.MAX_SHOOT_DURATION_SECONDS;
import static com.an.ffmpeg.widget.VideoTrimmerUtil.MIN_SHOOT_DURATION_SECONDS;
import static com.an.ffmpeg.widget.VideoTrimmerUtil.RECYCLER_VIEW_PADDING;
import static com.an.ffmpeg.widget.VideoTrimmerUtil.THUMBNAIL_SIZE;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_READY;

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
import com.an.file.FileManager;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSource;

import java.io.File;

public class VideoTrimmerView extends FrameLayout {

    private static final String TAG = VideoTrimmerView.class.getSimpleName();

    private ExoPlayer exoPlayer;
    private MediaSource mediaSource;
    private Context mContext;
    private StyledPlayerView mVideoView;
    private ImageView mPlayView;
    private ImageView soundView;
    private RecyclerView mVideoThumbRecyclerView;
    private RangeSeekBarView mRangeSeekBarView; //裁剪选择框
    private LinearLayout mSeekBarLayout;
    private TextView selectTimeView;
    private File inFile;
    private File outFile;
    private int mDuration = 0; //视频总时长
    private VideoTrimListener mOnTrimVideoListener;
    private VideoTrimmerAdapter mVideoThumbAdapter;
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
        mVideoView = findViewById(R.id.video_loader);
        mPlayView = findViewById(R.id.icon_video_play);
        soundView = findViewById(R.id.sound_view);
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
        mPlayView.setOnClickListener(v -> videoPlayOrPause());
        soundView.setOnClickListener(v -> soundOnOrOff());
    }

    private void soundOnOrOff() {
        if (exoPlayer.getVolume() > 0) {
            exoPlayer.setVolume(0);
        } else {
            exoPlayer.setVolume(0.3f);
        }
    }

    private void onCancelClicked() {
        mOnTrimVideoListener.onCancel();
    }


    public void setOnTrimVideoListener(VideoTrimListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }


    private void onSaveClicked() {
        if (mRangeSeekBarView.getSelectTime() < MIN_SHOOT_DURATION_SECONDS) {
            Toast.makeText(mContext, getResources().getString(R.string.video_shoot_min_tip), Toast.LENGTH_SHORT).show();
        } else {
            videoStop();
            VideoTrimmerUtil.trim(mContext,
                    inFile.getPath(),
                    getContext().getFilesDir().getAbsolutePath(),
                    mRangeSeekBarView.getSelectedLeftTimeInVideo() * 1000,
                    mRangeSeekBarView.getSelectedRightTimeInVideo() * 1000,
                    mOnTrimVideoListener);
        }
    }


    public void initVideoByURI(final File inFile, final File outFile) {
        this.inFile = inFile;
        this.outFile = outFile;
        exoPlayer = new ExoPlayer
                .Builder(getContext())
                .build();
        mediaSource = createMediaSource(getContext(), inFile.getAbsolutePath());
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == STATE_READY) {
                    videoPrepared(exoPlayer.getDuration());
                } else if (playbackState == STATE_ENDED) {
                    videoCompleted();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                setPlayPauseViewIcon(isPlaying);
            }

            @Override
            public void onVolumeChanged(float volume) {
                setVolumeView(volume);
            }
        });
        mVideoView.requestFocus();
        mVideoView.setPlayer(exoPlayer);
        exoPlayer.prepare();
    }

    private void setVolumeView(float volume) {
        if (volume > 0) {
            soundView.setImageResource(R.drawable.on);
        } else {
            soundView.setImageResource(R.drawable.off);
        }
    }

    private MediaSource createMediaSource(Context context, String uri) {
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context.getApplicationContext());
        MediaItem mediaItem = MediaItem.fromUri(uri);
        return new ProgressiveMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
    }


    private void videoPrepared(long videoDuration) {
        Log.d(TAG, "videoDuration=" + videoDuration);
        mDuration = (int) (videoDuration / 1000);
        initRangeSeekBarView(mDuration);
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
        startShootVideoThumbs(mContext, inFile, mThumbsTotalCount, 0, durationSeconds);
    }

    /**
     * 获取预览帧
     **/
    private void startShootVideoThumbs(final Context context, final File inFile, int totalThumbsCount, long startPosition, long endPosition) {
        VideoTrimmerUtil.shootVideoThumbInBackground(context, inFile, totalThumbsCount, startPosition, endPosition,
                (bitmap, interval) -> {
                    if (bitmap != null) {
                        UiThreadExecutor.runTask("", () -> mVideoThumbAdapter.addBitmaps(bitmap), 0L);
                    }
                });
    }

    private void videoCompleted() {
        setPlayPauseViewIcon(false);
    }

    private void videoStart() {
        exoPlayer.play();
        mRangeSeekBarView.playingProgressAnimation();
    }

    private void videoStop() {
        exoPlayer.pause();
        mRangeSeekBarView.pauseProgressAnimation();
    }

    private void videoResume() {
        int startPosition = (int) (exoPlayer.getCurrentPosition() / 1000);
        if (startPosition >= mRangeSeekBarView.getSelectedRightTimeInVideo()) {
            startPosition = mRangeSeekBarView.getSelectedLeftTimeInVideo();
        }
        seekTo(startPosition);
        videoStart();
    }

    private void videoPlayOrPause() {
        if (exoPlayer.isPlaying()) {
            videoStop();
        } else {
            videoResume();
        }
    }


    private void seekTo(int seconds) {
        seconds = seconds * 1000;
        exoPlayer.seekTo(seconds);
    }

    private void setPlayPauseViewIcon(boolean isPlaying) {
        mPlayView.setImageResource(isPlaying ? R.drawable.play : R.drawable.pause);
        if (isPlaying) {
            mPlayView.postDelayed(() -> mPlayView.setVisibility(GONE), 1000L);
        } else {
            mPlayView.setVisibility(VISIBLE);
        }
    }

    private final RangeSeekBarView.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = (leftSelectTime, rightSelectTime, action, pressedThumb) -> {
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                videoStop();
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
                videoStart();
                break;
            default:
                videoStop();
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
            float percent = scrollPercent();
            long scrollTimeSecond = (long) (percent * mDuration);
            Log.d(TAG, "percent=" + percent + " mDuration=" + mDuration + " scrollTimeSecond=" + scrollTimeSecond);

            mRangeSeekBarView.setStartTimeInVideo(scrollTimeSecond);
        }
    };

    /**
     * 水平滑动的距离占总长度的百分比
     */
    private float scrollPercent() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mVideoThumbRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        float scrollX = (position) * itemWidth - firstVisibleChildView.getLeft() + RECYCLER_VIEW_PADDING;
        Log.d(TAG, "scrollX=" + scrollX + " totalX=" + (mThumbsTotalCount * itemWidth));

        float percent = scrollX / (mThumbsTotalCount * itemWidth);
        return percent;
    }

    public void onResume() {
        videoResume();
    }

    public void onPause() {
        videoStop();
    }

    public void onDestroy() {
        exoPlayer.stop();
        exoPlayer.release();
    }
}
