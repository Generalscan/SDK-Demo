package com.generalscan.sdkapp.support.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by alexli on 12/12/2017.
 */
object DateTimeUtils {

    fun getCurrentDateTimeNumber(): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
        val date = Date()
        return dateFormat.format(date)
    }

    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date()
        return dateFormat.format(date)
    }

    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = Date()
        return dateFormat.format(date)
    }

    fun formatDate(millis: Long?, format: String): String {
        val dateFormat = SimpleDateFormat(format)
        val date = Date()
        date.time = millis!!
        return dateFormat.format(date)
    }

    fun formatDate(millis: Long?): String {
        val date = Date()
        date.time = millis!!
        return String.format("%tD %tT", date)
    }

    fun getMinutesDifference(timeStart: Long, timeStop: Long): Long {
        val diff = timeStop - timeStart

        return diff / (60 * 1000)
    }

}