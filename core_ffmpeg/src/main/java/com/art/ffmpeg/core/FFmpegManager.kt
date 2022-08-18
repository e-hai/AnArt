package com.art.ffmpeg.core

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import java.util.*


object FFmpegManager {

    private const val TAG = "FFmpegManager"

    init {
        printVersion()
    }

    private fun printVersion() {
        val session = FFmpegKit.execute("-version")
        if (ReturnCode.isSuccess(session.returnCode)) {
            Log.d(TAG, "SUCCESS")
        } else if (ReturnCode.isCancel(session.returnCode)) {
            Log.d(TAG, "CANCEL")
        } else {
            Log.d(
                TAG,
                String.format("Command failed with state ${session.state} and rc ${session.returnCode}.${session.failStackTrace}")
            )
        }
    }

    /**
     * 视频 时长&尺寸裁剪
     * @param inputVideoUri 待裁剪的视频
     * @param outputVideoUri 得到的视频
     * @param start 开始时间（秒）
     * @param duration 裁剪时长（秒）从start开始算
     */
    fun cropVideo(
        context: Context,
        inputVideoUri: Uri,
        outputVideoUri: Uri,
        start: Int,
        duration: Int,
        callback: FFmpegCallback
    ) {
        val inputVideoPath = FFmpegKitConfig.getSafParameterForRead(context, inputVideoUri)
        val outputVideoPath = FFmpegKitConfig.getSafParameterForWrite(context, outputVideoUri)
        val frameRate = 24
        val maxWidth = 720
        val maxHeight = 720
        //-ss 0 -t 5  时间裁切
        //-strict -2 -vf crop=500:500:0:100   尺寸裁切
        val cmd = " -y -i $inputVideoPath" +
                " -ss $start" +
                " -t $duration" +
                " -vf scale=w=$maxWidth:h=$maxHeight:force_original_aspect_ratio=decrease" +
                "  -r $frameRate $outputVideoPath"
        Log.d(TAG,cmd)
        executeCommand(cmd, callback)
    }

    private fun executeCommand(
        command: String, callback: FFmpegCallback
    ) {
        FFmpegKit.executeAsync(command,
            {
                val state = if (ReturnCode.isSuccess(it.returnCode)) {
                    Log.d(TAG, "Success")
                    State.Success
                } else if (ReturnCode.isCancel(it.returnCode)) {
                    Log.d(TAG, "Cancel")
                    State.Cancel
                } else {
                    Log.d(
                        TAG,
                        "Command failed with state ${it.state} and rc ${it.returnCode}.${it.failStackTrace}"
                    )
                    State.Fail
                }
                callback.onComplete(state)
            },
            {
                // CALLED WHEN SESSION PRINTS LOGS
                Log.d(TAG, "${it.sessionId} ${it.level} ${it.message}")
            },
            {
                // CALLED WHEN SESSION GENERATES STATISTICS
                Log.d(TAG, "${it.sessionId} ${it.size}")
            })
    }
}