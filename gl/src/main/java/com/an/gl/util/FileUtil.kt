package com.an.gl.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileUtil {

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