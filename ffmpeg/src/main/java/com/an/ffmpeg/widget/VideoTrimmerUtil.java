package com.an.ffmpeg.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.an.ffmpeg.code.VideoFFCrop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class VideoTrimmerUtil {

    private static final String TAG = VideoTrimmerUtil.class.getSimpleName();
    public static final long MIN_SHOOT_DURATION_SECONDS = 3;     // 最小剪辑时间3s
    public static final long MAX_SHOOT_DURATION_SECONDS = 15;    // 最小剪辑时间15s
    public static final int MAX_COUNT_RANGE = 5;                 //seekBar的区域内一共有多少张图片
    public static final int THUMBNAIL_SIZE = Utils.dpToPx(72);   //一张图片的宽、高
    public static final int RECYCLER_VIEW_PADDING = Utils.dpToPx(36);

    public static void trim(Context context, String srcVideo, String destPath, long startMs, long endMs, final VideoTrimListener callback) {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final String outputName = "trimmedVideo_" + timeStamp + ".mp4";
        destPath = destPath + "/" + outputName;
        long duration = endMs - startMs;
        VideoFFCrop.Companion.getInstance().cropVideo(context, srcVideo, destPath, (int) startMs/1000, (int) duration/1000, new VideoFFCrop.FFListener() {
            @Override
            public void onFail(@Nullable String msg) {
                Log.d(TAG, "onFail");
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish");

            }

            @Override
            public void onProgress(@Nullable Integer progress) {
                Log.d(TAG, "onProgress");

            }
        });
    }

    public static void shootVideoThumbInBackground(final Context context, final File videoUri, final int totalThumbsCount, final long startPosition,
                                                   final long endPosition, final SingleCallback<Bitmap, Integer> callback) {
        BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0L, "") {
            @Override
            public void execute() {
                try {
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    mediaMetadataRetriever.setDataSource(context, Uri.fromFile(videoUri));
                    // Retrieve media data use microsecond
                    long interval = (endPosition - startPosition) / (totalThumbsCount - 1);
                    Log.d(TAG, "interval=" + interval
                            + " startPosition=" + startPosition
                            + " endPosition=" + endPosition
                            + " totalThumbsCount=" + totalThumbsCount
                    );

                    for (long i = 0; i < totalThumbsCount; ++i) {
                        long frameTime;
                        if (0 == interval) {
                            frameTime = startPosition + i;
                        } else {
                            frameTime = startPosition + interval * i;
                        }
                        frameTime = frameTime * 1000 * 1000;
                        Log.d(TAG, "frameTime=" + frameTime);

                        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(frameTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        if (bitmap == null) continue;
                        try {
                            bitmap = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, false);
                        } catch (final Throwable t) {
                            t.printStackTrace();
                        }
                        callback.onSingleCallback(bitmap, (int) interval);
                    }
                    mediaMetadataRetriever.release();
                } catch (final Throwable e) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        });
    }

    public static String getVideoFilePath(String url) {
        if (TextUtils.isEmpty(url) || url.length() < 5) return "";
        if (url.substring(0, 4).equalsIgnoreCase("http")) {

        } else {
            url = "file://" + url;
        }

        return url;
    }

    private static String convertSecondsToTime(long seconds) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (seconds <= 0) {
            return "00:00";
        } else {
            minute = (int) seconds / 60;
            if (minute < 60) {
                second = (int) seconds % 60;
                timeStr = "00:" + unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99) return "99:59:59";
                minute = minute % 60;
                second = (int) (seconds - hour * 3600 - minute * 60);
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    private static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10) {
            retStr = "0" + Integer.toString(i);
        } else {
            retStr = "" + i;
        }
        return retStr;
    }
}