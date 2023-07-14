package com.art.ffmpeg.extensions.crop

import android.content.Context

fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density + 0.5f).toInt()
}

fun Int.spToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.scaledDensity + 0.5f).toInt()
}


/**
 * second to HH:MM:ss
 *
 * @param seconds
 * @return
 */
fun convertSecondsToTime(seconds: Long): String {
    if (seconds <= 0) return "00:00"

    var timeStr = ""
    var minute: Int = seconds.toInt() / 60
    if (minute < 60) {
        val second = seconds.toInt() % 60
        timeStr = unitFormat(minute) + ":" + unitFormat(second)
    } else {
        val hour = minute / 60
        if (hour > 99) return "99:59:59"
        minute %= 60
        val second = (seconds - hour * 3600 - minute * 60).toInt()
        timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second)
    }
    return timeStr
}

private fun unitFormat(i: Int): String {
    return if (i in 0..9) "0$i" else "" + i
}