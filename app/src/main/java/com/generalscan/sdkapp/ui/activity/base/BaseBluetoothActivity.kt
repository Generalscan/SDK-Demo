package com.generalscan.sdkapp.ui.activity.base

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

/**
 * Created by Alex on 9/10/2016.
 */

open class BaseBluetoothActivity : BaseActivity() {

    protected var mBluetoothAdapter: BluetoothAdapter? = null
    protected var pairedDevices: Set<BluetoothDevice>? = null

    companion object {
        val UUID_ANDROID_DEVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
