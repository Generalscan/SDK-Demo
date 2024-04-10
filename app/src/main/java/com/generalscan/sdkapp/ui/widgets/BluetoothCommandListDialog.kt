package com.generalscan.sdkapp.ui.widgets

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.generalscan.scannersdk.core.basic.consts.SdkConstants

import com.generalscan.scannersdk.core.scannercommand.commandcontrol.CommandController
import com.generalscan.sdkapp.R

/**
 * 发送数据列表
 *
 * @author Administrator
 */
class BluetoothCommandListDialog(private val mContext: Context, private val bluetoothCommandController: CommandController) {

    // 所有的发送内容
    // All send
    private val AllConstant = intArrayOf(
            SdkConstants.SCANNER_COMMAND_TYPE_FUNCTION,
            //SdkConstants.SCANNER_COMMAND_TYPE_CONFIG,
            SdkConstants.SCANNER_COMMAND_TYPE_READ)

    /**
     * 显示所有发送数据的对话框 Display Dialog of send data
     */
    fun ShowoDialog() {

        // 对应发送内容的描述
        // The corresponding transmitting content description
        val items = arrayOf<CharSequence>(
                mContext.getString(R.string.gs_command_type_function),
                //mContext.getString(R.string.gs_command_type_config),
                mContext.getString(R.string.gs_command_type_read))

        val builer = AlertDialog.Builder(mContext)
        builer.setSingleChoiceItems(items, -1
        ) { dialog, which ->
            bluetoothCommandController.sendCommand(mContext, AllConstant[which])
            dialog.cancel()
        }
        builer.create().show()
    }
}
