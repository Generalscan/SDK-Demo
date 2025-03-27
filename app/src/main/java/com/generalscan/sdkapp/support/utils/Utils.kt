package com.generalscan.sdkapp.support.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import java.io.File
import java.util.*

/**
 * Created by alexli on 23/2/2018.
 */

object Utils
{
    fun stackTraceToString(e: Throwable): String {
        val sb = StringBuilder()
        for (element in e.stackTrace) {
            sb.append(element.toString())
            sb.append("\n")
        }
        return sb.toString()
    }

    fun getErrorMessage(e: Throwable): String {
        if (e.message == null)
            return e.toString()
        else
            return e.message.ifNullTrim()
    }
}
