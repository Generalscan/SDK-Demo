package com.generalscan.sdkapp.support.utils

import android.os.Build
import com.generalscan.sdkapp.system.base.AppContext
import com.generalscan.scannersdk.core.basic.interfaces.ILogger
import java.io.FileWriter

/**
 * Created by Alex Li on 1/8/2016.
 */
class ScannerLogger : ILogger {
    override fun logDataTransaction(message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Synchronized
    override fun logError(title: String, exception: Throwable) {
        logError(title, Utils.getErrorMessage(exception))
        if (exception.stackTrace != null) {
            for (sti in exception.stackTrace) {
                writeLog(sti.toString())
            }
        }
    }

    @Synchronized
    override fun logError(title: String, errorMessage: String) {
        writeLogHeader(title)
        writeLog(errorMessage)
    }

    @Synchronized
    override fun logInfo(title: String, message: String) {
        writeLogHeader(title)
        writeLog(message)
    }

    private fun writeLogHeader(title: String) {
        writeLog("===============$title=================")
        writeLog("Android Version:${Build.VERSION.RELEASE}")
        writeLog("App Version:" + AppContext.instance.appVersionCode)
    }

    @Synchronized
    private fun writeLog(log: String) {
        try {
            FileWriter(String.format(
                    AppContext.LOG_FILE_SCANNER, DateTimeUtils.getCurrentDate()), true).use {
                it.write("[" + DateTimeUtils.getCurrentDateTime() + "]:" + log + "\r\n")
                it.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    //endregion
}
