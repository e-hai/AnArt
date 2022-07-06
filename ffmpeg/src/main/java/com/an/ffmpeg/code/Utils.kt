package com.an.ffmpeg.code

import android.content.Context

object Utils {


    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    fun spToPx(context: Context, sp: Int): Int {
        return (sp * context.resources.displayMetrics.scaledDensity + 0.5f).toInt()
    }


    /**
     * second to HH:MM:ss
     *
     * @param seconds
     * @return
     */
    fun convertSecondsToTime(seconds: Long): String {
        var timeStr = ""
        val hour: Int
        var minute: Int
        val second: Int
        if (seconds <= 0) return "00:00" else {
            minute = seconds.toInt() / 60
            if (minute < 60) {
                second = seconds.toInt() % 60
                timeStr = unitFormat(minute) + ":" + unitFormat(second)
            } else {
                hour = minute / 60
                if (hour > 99) return "99:59:59"
                minute %= 60
                second = (seconds - hour * 3600 - minute * 60).toInt()
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second)
            }
        }
        return timeStr
    }

    private fun unitFormat(i: Int): String {
        return if (i in 0..9) "0$i" else "" + i
    }
}