package com.an.ffmpeg.code

import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

object VideoCrop {

    private const val TAG = "VideoCrop"

    /**
     * 初始化
     */
    suspend fun init(context: Application) = withContext(Dispatchers.IO) {
        var inputSrc: InputStream? = null
        var outDest: FileOutputStream? = null
        try {
            val ffmpegPath = context.applicationInfo.nativeLibraryDir + "/ffmpeg.so"
            val ffmpegFile = File(ffmpegPath)

            inputSrc = context.assets.open("ffmpeg.so")
            if (ffmpegFile.exists() && inputSrc.available().toLong() == ffmpegFile.length()) {
                return@withContext
            }
            outDest = FileOutputStream(ffmpegPath)
            val buf = ByteArray(96 * 1024)
            var length: Int
            while (inputSrc.read(buf).also { length = it } != -1) {
                outDest.write(buf, 0, length)
            }
            outDest.flush()
            outDest.close()
            inputSrc.close()

            Runtime.getRuntime().exec("/system/bin/chmod 755 $ffmpegPath").waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (inputSrc != null) {
                try {
                    inputSrc.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (outDest != null) {
                try {
                    outDest.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 视频 时长&尺寸裁剪
     * @param srcFilePath 待裁剪的视频
     * @param destFilePath 得到的视频
     * @param start 开始时间（秒）
     * @param duration 裁剪时长（秒）从start开始算
     */
    fun cropVideo(
        context: Context,
        srcFilePath: String,
        destFilePath: String,
        start: Int,
        duration: Int
    ) = flow {
        val frameRate = 24
        val width = 720
        val height = 720
        //-ss 0 -t 5  时间裁切
        //-strict -2 -vf crop=500:500:0:100   尺寸裁切
        val cmd = (context.applicationInfo.nativeLibraryDir
                + "/ffmpeg.so"
                + " -y -i "
                + "" + srcFilePath + ""        //加引号避免名字有空格无法识别
                + " -ss " + start
                + " -t " + duration
                + " -vf scale=w=$width:h=$height:force_original_aspect_ratio=decrease "
//                + " -c copy "                //直接复制一段，不能与-r一起使用
                + " -r $frameRate "            //帧数修改为24
                + "" + destFilePath + "")      //加引号避免名字有空格无法识别

        val resultCode = try {
            val ffmpegCmd = Runtime.getRuntime().exec(cmd)
            val error = ffmpegCmd.errorStream
            val errorScanner = Scanner(error)
            var count = 0
            while (errorScanner.hasNextLine()) {
                val line = errorScanner.nextLine()
                Log.d(TAG, line)
                ++count
                val fz = count
                val fm = duration * 1000 / 100
                var progress: Int = fz.times(100) / fm
                progress = if (progress > 99) 99 else progress
                emit(VideoCropStatus.Loading(progress))
            }
            ffmpegCmd.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            200
        }
        if (resultCode == 0) {
            emit(VideoCropStatus.Loading(100))
            emit(VideoCropStatus.Success(destFilePath))
        } else {
            emit(VideoCropStatus.Fail("crop exception "))
        }
    }
}


sealed class VideoCropStatus {
    object Idle : VideoCropStatus()
    data class Success(val destFilePath: String) : VideoCropStatus()
    data class Fail(val msg: String) : VideoCropStatus()
    data class Loading(val progress: Int) : VideoCropStatus()
}
