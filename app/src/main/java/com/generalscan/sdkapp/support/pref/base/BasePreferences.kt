package com.generalscan.sdkapp.support.pref.base

import android.content.SharedPreferences
import com.generalscan.sdkapp.support.kotlinext.ifNullOrEmpty
import com.generalscan.sdkapp.system.base.AppContext

/**
 * Created by alexli on 17/5/2017.
 */

open class BasePreferences {
    companion object {

        val appInstance: AppContext
            get() = AppContext.instance

        val appPrefs: SharedPreferences
            get() = AppContext.instance.appPreferences.appPrefs!!

        fun getString(resKey:String, defaultValue: String = ""): String {
            return appPrefs.getString(resKey, defaultValue).ifNullOrEmpty("")
        }

        fun putString(resKey:String, value: String) {
            appPrefs.edit().putString(resKey, value).apply()
        }

        fun getBoolean(resKey:String, defaultValue: Boolean = false): Boolean {
            return appPrefs.getBoolean(resKey, defaultValue)
        }

        fun putBoolean(resKey:String, value: Boolean) {
            appPrefs.edit().putBoolean(resKey, value).apply()
        }

        fun getLong(resKey:String, defaultValue: Long = 0L): Long {
            return appPrefs.getLong(resKey, defaultValue)
        }

        fun putLong(resKey:String, value: Long?) {
            appPrefs.edit().putLong(resKey, value!!).apply()
        }

        fun getInt(resKey:String, defaultValue: Int = 0): Int {
            return try {
                appPrefs.getInt(resKey, defaultValue)
            } catch (e: Throwable) {
                e.printStackTrace()
                defaultValue
            }
        }

        fun putInt(resKey:String, value: Int?) {
            appPrefs.edit().putInt(resKey, value!!).apply()
        }
    }
}
