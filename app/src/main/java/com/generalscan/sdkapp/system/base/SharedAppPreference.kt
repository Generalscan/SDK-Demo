package com.generalscan.sdkapp.system.base

/**
* Created by Alex Li on 27/7/2016.
*/

import android.content.Context
import android.content.SharedPreferences

class SharedAppPreference(var context: Context) {

    var appPrefs: SharedPreferences = context.applicationContext.getSharedPreferences("GsSdkDemoApp", 0)

}