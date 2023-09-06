package com.an.gl.plus.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


object FileUtil {

     fun getBitmap(res: Resources, drawableRes: Int): Bitmap? {
        val drawable: Drawable = res.getDrawable(drawableRes, null)
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    fun createFile(context: Context, fileName: String): File {
        return File(context.filesDir, fileName).apply {
            createNewFile()
        }
    }

    fun createFileByAssets(context: Context, fileName: String, assetName: String): File {
        val file = File(context.filesDir, fileName).apply {
            createNewFile()
        }
        val inputStream = context.assets.open(assetName)
        val outputStream = FileOutputStream(file)
        try {
            val fileReader = ByteArray(4096)
            while (true) {
                val read = inputStream.read(fileReader)
                if (read == -1) {
                    break
                }
                outputStream.write(fileReader, 0, read)
            }
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream.close()
            outputStream.close()
        }
        return file
    }

}