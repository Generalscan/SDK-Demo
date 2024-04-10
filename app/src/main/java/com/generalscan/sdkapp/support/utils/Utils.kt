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
    /**
     * Compares two version strings.
     *
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     * The result is a positive integer if str1 is _numerically_ greater than str2.
     * The result is zero if the strings are _numerically_ equal.
     */
    fun versionCompare(str1: String, str2: String): Int {
        val vals1 = str1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val vals2 = str2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var i = 0
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.size && i < vals2.size && vals1[i] == vals2[i]) {
            i++
        }
        // compare first non-equal ordinal number
        if (i < vals1.size && i < vals2.size) {
            val diff = Integer.valueOf(vals1[i])!!.compareTo(Integer.valueOf(vals2[i]))
            return Integer.signum(diff)
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        return Integer.signum(vals1.size - vals2.size)
    }


    //endregion

    //region Datetime functions
    val randomID: Long
        get() {
            val ra = Random()
            return ra.nextLong()
        }

    val uuid: String
        get() {
            val uuid = UUID.randomUUID()
            return uuid.toString()
        }

    //region File process



    //endregion

    fun stackTraceToString(e: Throwable): String {
        val sb = StringBuilder()
        for (element in e.stackTrace) {
            sb.append(element.toString())
            sb.append("\n")
        }
        return sb.toString()
    }

    fun isAppInDebugMode(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.state == NetworkInfo.State.CONNECTED
    }

    fun isWifiOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return (networkInfo != null
                && networkInfo.type == ConnectivityManager.TYPE_WIFI
                && networkInfo.state == NetworkInfo.State.CONNECTED)
    }


    fun getErrorMessage(e: Throwable): String {
        if (e.message == null)
            return e.toString()
        else
            return e.message.ifNullTrim()
    }

    fun getHtmlBoldColorText(src: String, color: String): String {
        return "<b><font color='$color'>$src</font></b>"
    }

    fun isNumeric(str: String): Boolean {
        return str.matches(("-?\\d+(\\.\\d+)?").toRegex())  //match a number with optional '-' and decimal.
    }

    fun isInteger(str: String): Boolean {
        return str.matches(("^[+-]?\\d+$").toRegex())  //match a number with optional '-' and decimal.
    }


    @SuppressLint("MissingPermission")
    fun getDeviceId(context: Context): String {
        val telephonyManager = context
                .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val imei = telephonyManager!!.deviceId
        return imei
    }

    fun isAppInstalled(packageName: String): Boolean {
        return File("/data/data/" + packageName).exists()
    }

}
