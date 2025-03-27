package com.generalscan.sdkapp.support.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by alexli on 12/12/2017.
 */
object DateTimeUtils {



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

}