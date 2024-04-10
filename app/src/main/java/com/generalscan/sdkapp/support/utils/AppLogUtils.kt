package com.generalscan.sdkapp.support.utils

import android.os.Build
import android.util.Log
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import com.generalscan.sdkapp.support.task.CallResult
import com.generalscan.sdkapp.system.base.AppContext
import java.io.File
import java.io.FileWriter

/**
* Created by Alex Li on 1/8/2016.
*/
object AppLogUtils {
    //region Log functions

    fun sysLog(e: Throwable) {
        Log.e("MobileFMS_Log", Utils.getErrorMessage(e))
        Log.e("MobileFMS_Log", Utils.stackTraceToString(e))
    }

    fun sysLog(message: String) {
        Log.e("MobileFMS_Log", message)
    }

    fun logError(logTitle: String, e: Throwable) {
        logInfo("===============$logTitle=================")
        logInfo("Android Version:" + Build.VERSION.RELEASE)
        //i("App Version:" + SecurityReferences.appVersion)
        logInfo(e.message.ifNullTrim())
        for (sti in e.stackTrace) {
            logInfo(sti.toString())
        }
    }


    fun logInfo(log: String) {
        try {
            val writer = FileWriter(String.format(
                    AppContext.LOG_FILE, DateTimeUtils.getCurrentDate()), true)
            writer.write("[" + DateTimeUtils.getCurrentDateTime() + "]:" + log + "\r\n")
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun removeOldLogEntries(): CallResult {
        val result = CallResult()
        try {
            val logDir = File(AppContext.LOG_FOLDER)
            IOUtils.clearFolder(logDir, 3)
        } catch (e: Exception) {
            // ex.printStackTrace();
            logError("Upload Log File", e)
            sysLog(e)
            result.errorMessage = Utils.getErrorMessage(e)
        }

        return result
    }
    //endregion
}
