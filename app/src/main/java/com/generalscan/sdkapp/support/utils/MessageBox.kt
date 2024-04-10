package com.generalscan.sdkapp.support.utils

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.DialogInterface
import android.content.Intent

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.support.task.CallResult
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Created by Alex Li on 27/7/2016.
 */
object MessageBox {

    fun newAlertDialogBuilder(context: Context): AlertDialog.Builder {
        return AlertDialog.Builder(context)
    }

    fun newAlertDialogBuilder(context: Context,
                                      title: CharSequence?, message: CharSequence): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
        if (title != null) {
            builder.setTitle(title)
        }
        return builder
    }

    fun showToastMessage(ctx: Context, message: String) {

        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
    }

    fun showToastMessage(ctx: Context, messageResId: Int) {

        showToastMessage(ctx, ctx.getString(messageResId))
    }

    //region Show Warning Message
    fun showWarningMessage(context: Context, messageResid: Int, callback: (() -> Unit)? = null) {
        showWarningMessage(context, context.getString(messageResid), callback)
    }

    fun showWarningMessage(context: Context, message: CharSequence, callback: (() -> Unit)? = null) {
        showWarningMessage(context, message, null, callback)
    }

    fun showWarningMessage(context: Context, e: Exception, callback: (() -> Unit)? = null) {
        val message = Utils.getErrorMessage(e)
        var stackTrace: String? = null
        if (e.stackTrace != null && e.stackTrace.isEmpty()) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            stackTrace = sw.toString()
        }
        showWarningMessage(context, message, stackTrace, callback)
    }

    fun showWarningMessage(context: Context, message: CharSequence, stackTrace: CharSequence? = null, callback: (() -> Unit)? = null) {
        val builder = getWarningMessageBoxBuilder(context, message, callback)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun showInfoMessage(context: Context, messageResId: Int, callback: (() -> Unit)? = null) {
        showInfoMessage(context, context.getString(messageResId), callback)
    }

    fun showInfoMessage(context: Context, message: String, callback: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(context)
        val alertDialog = builder
                .setIcon(R.drawable.ic_information)
                .setTitle(context.getString(R.string.dialog_info_title).toUpperCase())
                .setMessage(message)
                .setPositiveButton(context.getString(android.R.string.ok)) { dialog, which ->
                    //确定按钮
                    dialog.dismiss()
                    if (callback != null)
                        run {
                            callback()
                        }
                    //callback?.start(context)
                }
                .create()
        alertDialog.show()
    }

    fun showListBox(context: Context, title: String, items: Array<String>, callback: ((result: CallResult) -> Unit)?) {
        val itemListener = DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
            if (callback != null) {
                val result = CallResult()
                result.isSuccess = true
                result.result = items[which]
                run { callback(result) }
            }
        }
        val builder = AlertDialog.Builder(context)
        val alertDialog = builder
                .setIcon(R.drawable.ic_information)
                .setTitle(title)
                .setItems(items, itemListener)
                .create()
        alertDialog.show()
    }

    fun showConfirmBox(ctx: Context,
                       title: CharSequence,
                       message: CharSequence,
                       okText: CharSequence,
                       okListener: DialogInterface.OnClickListener,
                       cancelText: CharSequence,
                       cancelListener: DialogInterface.OnClickListener? = null) {
        val builder = newAlertDialogBuilder(ctx, title, message)
        builder.setPositiveButton(okText, okListener)
        builder.setNegativeButton(cancelText, cancelListener)
        builder.create().show()
    }

    fun showConfirmBox(ctx: Context,
                       titleId: Int, messageId: Int,
                       okTextId: Int,
                       okListener: DialogInterface.OnClickListener,
                       cancelTextId: Int,
                       cancelListener: DialogInterface.OnClickListener?) {
        showConfirmBox(ctx, ctx.getText(titleId), ctx.getText(messageId),
                ctx.getText(okTextId), okListener, ctx.getText(cancelTextId),
                cancelListener)
    }

    fun showMenuDialog(context: Context, targetView: View,
                       menuArr: Array<String?>, onItemClickListener: DialogInterface.OnClickListener) {

        AlertDialog.Builder(context)
                .setItems(menuArr, onItemClickListener)
                .show()

    }


    @SuppressLint("NotificationPermission")
    @JvmOverloads
    fun showNotification(context: Context, title: String, message: String, resultIntent: Intent? = null, iconResId: Int = R.drawable.ic_notification, colorResId: Int = R.color.colorPrimary) {
        try {
            val mBuilder = NotificationCompat.Builder(context)
            mBuilder.setSmallIcon(iconResId)
            mBuilder.setColor(ContextCompat.getColor(context, colorResId))
            mBuilder.setContentTitle("GSFirm:" + title)
            mBuilder.setContentText(message)
            if (resultIntent != null) {
                val resultPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
                mBuilder.setContentIntent(resultPendingIntent)
            }
            // Sets an ID for the notification
            val mNotificationId = 1
            // Gets an instance of the NotificationManager service
            val mNotifyMgr = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // Builds the notification and issues it.
            mNotifyMgr.notify(iconResId.toString(), mNotificationId, mBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
            AppLogUtils.logError("showNotification", e)
        }

    }

    //region helper functions
    private fun getWarningMessageBoxBuilder(context: Context, message: CharSequence, callback: (() -> Unit)?): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)

        builder
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.dialog_warning_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    if (callback != null)
                        run { callback() }
                }
        return builder
    }
    //endregion
}//endreigon
