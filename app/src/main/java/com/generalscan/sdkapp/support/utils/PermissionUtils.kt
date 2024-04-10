package com.generalscan.sdkapp.support.utils

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.generalscan.sdkapp.R

/**
 * Created by alexli on 10/4/2017.
 */

object PermissionUtils {
    fun verifyPermissions(grantResults: IntArray): Boolean {
        // At least one result must be checked.
        if (grantResults.size < 1) {
            return false
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    fun checkAndRequestPermission(context:Activity, permissions: Array<String>, message:String, requestCode: Int):Boolean {
        val shouldRequestPermission = shouldRequestPermissions(context, permissions)
        if (shouldRequestPermission) {
            val shouldShowExplanation = shouldShowRequestPermissionRationale(context, permissions)
            if (shouldShowExplanation) {
                val builder = MessageBox.newAlertDialogBuilder(
                    context, context.getString(R.string.permission_dialog_title),
                    message
                )
                builder.setPositiveButton(context.getString(R.string.action_grant_permission))
                { _: DialogInterface, _ ->
                    ActivityCompat
                        .requestPermissions(context, permissions, requestCode)

                }
                val dialog = builder.create()
                dialog.show()
                dialog.setCancelable(false)

            } else {
                ActivityCompat
                    .requestPermissions(context, permissions, requestCode)
            }
        }
        return shouldRequestPermission
    }
    fun getPermissionMessage(context: Context):String
    {
        if (Build.VERSION.SDK_INT >= 30)
            return context.getString(R.string.permission_dialog_message, context.packageManager.backgroundPermissionOptionLabel)
        else
            return context.getString(R.string.permission_dialog_message, context.getString(R.string.background_location_allow_all_the_time))

    }
    fun shouldShowRequestPermissionRationale(activity: Activity, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT < 23) { //Android M
            return false
        }

        var requestPermission = false
        for (permission in permissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                requestPermission = true
                break
            }
        }

        return requestPermission
    }

    fun shouldRequestPermissions(activity: Activity, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT < 23) { //Android M
            return false
        }

        var requestPermission = false
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermission = true
                break
            }
        }

        return requestPermission
    }


    @TargetApi(Build.VERSION_CODES.M)
    fun showOverlayPermissionDialog(context: Activity, requestCode: Int): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message_for_overlay)
                .setPositiveButton(R.string.action_grant_permission) { dialogInterface, i ->
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()))
                        context.startActivityForResult(intent, requestCode)
                    } catch (e: ActivityNotFoundException) {
                        MessageBox.showWarningMessage(context, "SYSTEM_ALERT_WINDOW")
                    }
                }

        val dialog = builder.create()
        dialog.show()
        dialog.setCancelable(false)

        return dialog
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun canDrawOverlays(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

}
