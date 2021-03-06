package com.an.ffmpeg.widget

import kotlin.jvm.JvmOverloads
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import android.view.LayoutInflater
import com.an.ffmpeg.R
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.an.ffmpeg.widget.RangeSeekBarView.OnRangeSeekBarChangeListener
import com.an.ffmpeg.widget.RangeSeekBarView.Thumb
import android.view.MotionEvent
import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.an.ffmpeg.code.Utils
import com.an.ffmpeg.code.VideoThumbItem
import com.an.ffmpeg.widget.Constant.MAX_COUNT_RANGE
import com.an.ffmpeg.widget.Constant.MAX_SHOOT_DURATION_SECONDS
import com.an.ffmpeg.widget.Constant.MIN_SHOOT_DURATION_SECONDS
import com.google.android.exoplayer2.MediaItem

class VideoCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSource: MediaSource
    private var videoView: StyledPlayerView
    private var playingView: ImageView
    private var soundView: ImageView
    private var videoThumbRecyclerView: RecyclerView
    private var seekBarLayout: LinearLayout
    private var selectTimeView: TextView
    private var videoThumbAdapter: VideoCropAdapter
    private var rangeSeekBarView: RangeSeekBarView? = null
    private var thumbsTotalCount = 0
    private var videoDurationSec = 0
    private val recyclerViewPadding = Utils.dpToPx(context, 36)
    private var videoCropViewListener: VideoCropViewListener? = null
    lateinit var srcVideo: File

    init {
        LayoutInflater.from(context).inflate(R.layout.video_crop_view, this, true)
        videoView = findViewById(R.id.video_loader)
        playingView = findViewById(R.id.icon_video_play)
        soundView = findViewById(R.id.sound_view)
        seekBarLayout = findViewById(R.id.seekBarLayout)
        selectTimeView = findViewById(R.id.select_time_view)
        videoThumbRecyclerView = findViewById(R.id.video_frames_recyclerView)
        videoThumbRecyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        videoThumbAdapter = VideoCropAdapter()
        videoThumbRecyclerView.adapter = videoThumbAdapter
        videoThumbRecyclerView.addOnScrollListener(buildOnScrollListener())
        initListeners()
    }

    private fun buildOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val percent = scrollPercent()
                val scrollTimeSecond = (percent * videoDurationSec).toLong()
                rangeSeekBarView?.setStartTimeInVideo(scrollTimeSecond)
            }
        }

    }

    private fun initListeners() {
        playingView.setOnClickListener { videoPlayOrPause() }
        soundView.setOnClickListener { soundOnOrOff() }
    }

    private fun soundOnOrOff() {
        if (exoPlayer.volume > 0) {
            exoPlayer.volume = 0f
        } else {
            exoPlayer.volume = 0.3f
        }
    }


    fun initVideoByUri(srcVideo: File, videoCropViewListener: VideoCropViewListener?) {
        this.srcVideo = srcVideo
        this.videoCropViewListener = videoCropViewListener
        exoPlayer = ExoPlayer.Builder(context)
            .build()
        mediaSource = createMediaSource(context, srcVideo.absolutePath)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    videoPrepared(exoPlayer.duration / 1000)
                } else if (playbackState == Player.STATE_ENDED) {
                    videoCompleted()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                setPlayPauseViewIcon(isPlaying)
            }

            override fun onVolumeChanged(volume: Float) {
                setVolumeView(volume)
            }
        })
        videoView.requestFocus()
        videoView.player = exoPlayer
        exoPlayer.prepare()
    }


    /**
     * ??????????????????
     */
    private fun initRangeSeekBarView(durationSeconds: Int) {
        if (rangeSeekBarView != null) return
        //???????????????????????????????????????????????????????????????????????????????????????????????????
        //?????????????????????????????????????????????????????????????????????????????????
        val maxShootDuration: Long
        if (durationSeconds <= MAX_SHOOT_DURATION_SECONDS) {
            thumbsTotalCount = MAX_COUNT_RANGE
            maxShootDuration = durationSeconds.toLong()
        } else {
            thumbsTotalCount =
                (durationSeconds * 1.0f / (MAX_SHOOT_DURATION_SECONDS * 1.0f) * MAX_COUNT_RANGE).toInt()
            maxShootDuration = MAX_SHOOT_DURATION_SECONDS
        }
        videoThumbRecyclerView.addItemDecoration(
            SpacesItemDecoration(
                recyclerViewPadding,
                thumbsTotalCount
            )
        )
        rangeSeekBarView =
            RangeSeekBarView(context, MIN_SHOOT_DURATION_SECONDS, maxShootDuration).apply {
                setOnRangeSeekBarChangeListener(buildRangeSeekBarChangeListener())
            }

        seekBarLayout.addView(rangeSeekBarView)
        startShootVideoThumbs(thumbsTotalCount, durationSeconds)
    }

    private fun buildRangeSeekBarChangeListener(): OnRangeSeekBarChangeListener {
        return object : OnRangeSeekBarChangeListener {
            override fun onRangeSeekBarChanged(
                leftSelectTime: Long,
                rightSelectTime: Long,
                action: Int,
                pressedThumb: Thumb?
            ) {
                when (action) {
                    MotionEvent.ACTION_MOVE -> {
                        videoStop()
                        val position: Long = if (pressedThumb == Thumb.MIN) {
                            leftSelectTime
                        } else {
                            rightSelectTime
                        }
                        seekTo(position.toInt())
                        updateSelectTime(leftSelectTime, rightSelectTime)
                    }
                    MotionEvent.ACTION_UP -> {
                        seekTo(leftSelectTime.toInt())
                        videoStart()
                    }
                    else -> videoStop()
                }
            }
        }
    }

    /**
     * ???????????????
     */
    private fun startShootVideoThumbs(
        totalThumbsCount: Int,
        endSec: Int
    ) {
        val startSec = 0
        videoCropViewListener?.onLoadThumbList(
            totalThumbsCount,
            srcVideo.absolutePath,
            startSec,
            endSec
        )
    }

    private fun createMediaSource(context: Context, uri: String): MediaSource {
        val dataSourceFactory = DefaultDataSource.Factory(context.applicationContext)
        val mediaItem = MediaItem.fromUri(uri)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }

    private fun videoPrepared(durationSec: Long) {
        videoDurationSec = durationSec.toInt()
        initRangeSeekBarView(videoDurationSec)
    }

    private fun videoCompleted() {
        setPlayPauseViewIcon(false)
    }

    private fun videoStart() {
        exoPlayer.play()
        rangeSeekBarView?.playingProgressAnimation()
    }

    private fun videoStop() {
        exoPlayer.pause()
        rangeSeekBarView?.pauseProgressAnimation()
    }

    private fun videoResume() {
        val rangeSeekBarView = rangeSeekBarView ?: return
        var startPosition = (exoPlayer.currentPosition / 1000).toInt()
        if (startPosition >= rangeSeekBarView.selectedRightTimeInVideo) {
            startPosition = rangeSeekBarView.selectedLeftTimeInVideo
        }
        seekTo(startPosition)
        videoStart()
    }

    private fun videoPlayOrPause() {
        if (exoPlayer.isPlaying) {
            videoStop()
        } else {
            videoResume()
        }
    }

    private fun seekTo(seconds: Int) {
        exoPlayer.seekTo(seconds.toLong() * 1000)
    }

    private fun setVolumeView(volume: Float) {
        if (volume > 0) {
            soundView.setImageResource(R.drawable.icon_sound_on)
        } else {
            soundView.setImageResource(R.drawable.icon_sound_off)
        }
    }

    private fun setPlayPauseViewIcon(isPlaying: Boolean) {
        playingView.setImageResource(if (isPlaying) R.drawable.play else R.drawable.pause)
        if (isPlaying) {
            playingView.postDelayed({ playingView.visibility = GONE }, 1000L)
        } else {
            playingView.visibility = VISIBLE
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateSelectTime(leftSelectTime: Long, rightSelectTime: Long) {
        selectTimeView.text = (Utils.convertSecondsToTime(leftSelectTime)
                + "/"
                + Utils.convertSecondsToTime(rightSelectTime))
    }

    /**
     * ?????????????????????????????????????????????
     */
    private fun scrollPercent(): Float {
        val layoutManager = videoThumbRecyclerView.layoutManager as LinearLayoutManager
        val position = layoutManager.findFirstVisibleItemPosition()
        val firstVisibleChildView = layoutManager.findViewByPosition(position) ?: return 0f
        val itemWidth = firstVisibleChildView.width
        val scrollX: Float =
            (position * itemWidth - firstVisibleChildView.left + recyclerViewPadding).toFloat()
        return scrollX / (thumbsTotalCount * itemWidth)
    }

    fun updateThumbs(thumbList: List<VideoThumbItem>) {
        videoThumbAdapter.updateThumbs(thumbList)
    }

    fun getCropStartTimeSec(): Int {
        return rangeSeekBarView?.selectedLeftTimeInVideo ?: 0
    }

    fun getCropEndTimeSec(): Int {
        return rangeSeekBarView?.selectedRightTimeInVideo ?: 0
    }

    fun onResume() {
        videoResume()
    }

    fun onPause() {
        videoStop()
    }

    fun onDestroy() {
        exoPlayer.stop()
        exoPlayer.release()
    }

}