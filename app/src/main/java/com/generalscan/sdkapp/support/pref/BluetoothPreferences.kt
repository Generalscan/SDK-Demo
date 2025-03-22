package com.generalscan.sdkapp.support.pref

import com.generalscan.sdkapp.support.pref.base.BasePreferences


/**
 * Created by alexli on 17/5/2017.
 */

class BluetoothPreferences : BasePreferences() {
    companion object {
        var deviceWhiteList: String
            get() = getString("BluetoothDeviceWhiteList")
            set(value) = putString("BluetoothDeviceWhiteList", value)

        var isAutoConnect: Boolean
            get() = getBoolean("IsBluetoothDeviceAutoConnect", true)
            set(value) = putBoolean("IsBluetoothDeviceAutoConnect", value)
    }
}
