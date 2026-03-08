package com.generalscan.sdkapp.system.pref

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
            get() = AppContext.instance.appPreferences.appPrefs

        fun getString(prefKey:String, defaultValue: String = ""): String {
            return appPrefs.getString(prefKey, defaultValue).ifNullOrEmpty("")
        }

        fun putString(prefKey:String, value: String) {
            appPrefs.edit().putString(prefKey, value).apply()
        }

        fun getBoolean(prefKey:String, defaultValue: Boolean = false): Boolean {
            return appPrefs.getBoolean(prefKey, defaultValue)
        }

        fun putBoolean(prefKey:String, value: Boolean) {
            appPrefs.edit().putBoolean(prefKey, value).apply()
        }

        fun getLong(prefKey:String, defaultValue: Long = 0L): Long {
            return appPrefs.getLong(prefKey, defaultValue)
        }

        fun putLong(prefKey:String, value: Long?) {
            appPrefs.edit().putLong(prefKey, value!!).apply()
        }

        fun getInt(prefKey:String, defaultValue: Int = 0): Int {
            return try {
                appPrefs.getInt(prefKey, defaultValue)
            } catch (e: Throwable) {
                e.printStackTrace()
                defaultValue
            }
        }

        fun putInt(prefKey:String, value: Int?) {
            appPrefs.edit().putInt(prefKey, value!!).apply()
        }
    }
}
