package com.generalscan.sdkapp.system.base

import android.app.Application

import com.generalscan.sdkapp.support.utils.IOUtils
import com.generalscan.scannersdk.core.basic.SdkContext
import com.generalscan.scannersdk.core.session.bluetooth.connect.BluetoothConnectSession
import android.content.pm.PackageManager
import com.generalscan.sdkapp.support.utils.ScannerLogger


class AppContext : Application() {
    private var hasBeenInitiated = false
    var appVersionName:String = ""
    var appVersionCode:Int = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        //init()
    }

    fun init() {
        try {
            val pInfo = this.packageManager.getPackageInfo(packageName, 0)
            appVersionName = pInfo.versionName
            appVersionCode = pInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }


        try {
            initialSystemFiles()
            SdkContext.initSdk(this, ScannerLogger())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        hasBeenInitiated = true
    }


    companion object {

        //region Folders
        var MAIN_FOLDER = ""

        var TEMP_FOLDER = ""

        var UPLOAD_FOLDER = ""

        var ARCHIVE_FOLDER = ""

        var LOG_FOLDER = ""

        var LOG_FILE = ""

        var LOG_FILE_SCANNER = ""

        //endregion
        lateinit var instance: AppContext
            private set
        //private lateinit var appPreferences: AppPreference

        fun initialSystemFiles() {
            MAIN_FOLDER = instance.filesDir.absolutePath;
            TEMP_FOLDER = "$MAIN_FOLDER/temp/"
            UPLOAD_FOLDER = "$MAIN_FOLDER/upload/"
            ARCHIVE_FOLDER = "$MAIN_FOLDER/archive/"
            LOG_FOLDER = "$MAIN_FOLDER/logs/"
            LOG_FILE = LOG_FOLDER + "log_%s.txt"
            LOG_FILE_SCANNER = LOG_FOLDER + "log_scanner_%s.txt"
            IOUtils.createFolder(MAIN_FOLDER)
            IOUtils.createFolder(LOG_FOLDER)
            IOUtils.createFolder(TEMP_FOLDER)
            IOUtils.createFolder(UPLOAD_FOLDER)
            IOUtils.createFolder(ARCHIVE_FOLDER)
        }
    }

}
