package com.generalscan.sdkapp.ui.activity.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import android.view.Menu
import android.view.MenuItem
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.support.task.AsTask
import com.generalscan.sdkapp.support.task.CallResult
import com.generalscan.sdkapp.support.utils.AppLogUtils
import com.generalscan.sdkapp.support.utils.MessageBox
import com.generalscan.sdkapp.support.utils.PermissionUtils
import com.generalscan.sdkapp.system.base.AppContext
import com.generalscan.sdkapp.ui.activity.usb.UsbHostSettingActivity
import com.generalscan.sdkapp.ui.activity.base.BaseActivity
import com.generalscan.sdkapp.ui.activity.bluetooth.BluetoothMainActivity
import com.generalscan.sdkapp.ui.activity.usb.UsbSerialMainActivity
import java.util.ArrayList


class MainActivity : BaseActivity(), ActivityCompat.OnRequestPermissionsResultCallback {


    private var mHasInitialized = false
    private var mHasDestroied = false


    //region override functions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissions.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        /*
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            //permissions.add(Manifest.permission.CAMERA)
        }
        if(!PermissionUtils.checkAndRequestPermission(this, permissions.toTypedArray(), PermissionUtils.getPermissionMessage(this),  REQUEST_USER_PERMISSION))
        {
            checkOverlayPermission()
        }

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_id_connect_bluetooth -> {
                val intent = Intent(this, BluetoothMainActivity::class.java)
                startActivityForResult(intent, REQUEST_CONNECT_BLUETOOTH_DEVICE)
            }
            R.id.menu_id_connect_usb-> {
                val intent = Intent(this, UsbSerialMainActivity::class.java)
                startActivityForResult(intent, REQUEST_START_USBHOST_SERIVCE)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, requestedPermissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_USER_PERMISSION ->
                // We have requested multiple permissions for contacts, so all of them need to be
                // checked.
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    checkOverlayPermission()
                } else {
                    MessageBox.showWarningMessage(this, R.string.permissions_not_granted)
                    {
                        this@MainActivity.finish()
                    }
                }
            REQUEST_BACKGROUND_PERMISSION ->
                // We have requested multiple permissions for contacts, so all of them need to be
                // checked.
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    // All required permissions have been granted, display contacts fragment.
                    MessageBox.showToastMessage(this, R.string.permissions_available)
                    checkOverlayPermission()
                } else {
                    MessageBox.showWarningMessage(this, R.string.permissions_not_granted)
                    {
                        this@MainActivity.finish()
                    }
                }
            REQUEST_OVERLAY_PERMISSION_CODE -> {
                proceedActivityCreation()
            }
            else -> super.onRequestPermissionsResult(requestCode, requestedPermissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CONNECT_BLUETOOTH_DEVICE -> {
                if (resultCode == Activity.RESULT_OK) {
                    initBluetoothCommListener()
                }
            }
            REQUEST_START_USBHOST_SERIVCE -> {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHasDestroied = true

    }
    //endregion


    private fun proceedActivityCreation() {
        AppContext.instance.init()

        mHasInitialized = true
        removeOldLogs()
    }

    private fun initBluetoothCommListener() {

    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                PermissionUtils.showOverlayPermissionDialog(this, REQUEST_OVERLAY_PERMISSION_CODE)
            } else {
                proceedActivityCreation()
            }
        } else {
            proceedActivityCreation()
        }
    }

    //region remove old logs
    private fun removeOldLogs() {
        AsTask(
                this@MainActivity,
                { context ->
                    AppLogUtils.removeOldLogEntries()
                },
                { context: Context, result: CallResult ->
                    if (!result.isSuccess) {
                        MessageBox.showToastMessage(context, result.errorMessage)
                    }
                },
                null
        ).go()
    }
    //endregion

    companion object{
        private var permissions: MutableList<String> = ArrayList()
        private const val REQUEST_CONNECT_BLUETOOTH_DEVICE = 10
        private const val REQUEST_START_USBHOST_SERIVCE = 20
        private const val REQUEST_USER_PERMISSION = 30
        private const val REQUEST_BACKGROUND_PERMISSION = 31

        private const val REQUEST_OVERLAY_PERMISSION_CODE = 40
    }
}
