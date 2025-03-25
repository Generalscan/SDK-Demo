package com.generalscan.sdkapp.support.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatEditText
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import com.generalscan.sdkapp.support.kotlinext.textTrim
import com.generalscan.sdkapp.support.pref.BluetoothPreferences

object LeUtils {

    /*
  Configure device whitelist
   */
    fun configureWhitelist(activity: AppCompatActivity) {
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView: View = inflater.inflate(R.layout.view_ble_whitelist_settings, null)

        val txtDeviceWhiteList = dialogView.findViewById<AppCompatEditText>(R.id.edittext_bluetooth_device_whitelist)
        txtDeviceWhiteList.setText(BluetoothPreferences.deviceWhiteList)
        val ckbAutoConnect = dialogView.findViewById<AppCompatCheckBox>(R.id.checkbox_auto_connect)
        ckbAutoConnect.isChecked = BluetoothPreferences.isAutoConnect

        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            try {
                BluetoothPreferences.isAutoConnect = ckbAutoConnect.isChecked
                BluetoothPreferences.deviceWhiteList = txtDeviceWhiteList.textTrim
            } catch (e: Exception) {
                e.printStackTrace()
                MessageBox.showWarningMessage(activity, e.message.ifNullTrim())
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }
}