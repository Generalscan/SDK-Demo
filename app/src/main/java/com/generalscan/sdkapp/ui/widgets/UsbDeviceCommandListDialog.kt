package com.generalscan.sdkapp.ui.widgets

import android.annotation.SuppressLint
import android.app.AlertDialog.Builder
import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AlertDialog
import com.generalscan.scannersdk.core.basic.consts.SdkConstants
import com.generalscan.scannersdk.core.scannercommand.command.basic.ScannerCommand

import com.generalscan.scannersdk.core.scannercommand.commandcontrol.CommandController
import com.generalscan.scannersdk.core.scannercommand.commandgroup.CommandGroup
import com.generalscan.scannersdk.core.scannercommand.layout.ui.dialog.BaseDialog
import com.generalscan.scannersdk.core.session.usbhost.connect.UsbHostConnectSession
import com.generalscan.sdkapp.R

/**
 * 发送数据列表
 *
 * @author Administrator
 */
class UsbDeviceCommandListDialog(private val context: Context, private val callBack:(command: ScannerCommand)->Unit) {

    private var connectSession = UsbHostConnectSession(context, false)
    private var bluetoothCommandController: CommandController =
        CommandController(connectSession, callBack)
    init {
        connectSession.commandController = bluetoothCommandController
    }
    // 所有的发送内容
    // All send
    private val COMMAND_TYPES = intArrayOf(
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
                context.getString(R.string.gs_command_type_function),
                //mContext.getString(R.string.gs_command_type_config),
                context.getString(R.string.gs_command_type_read))

        val builer = AlertDialog.Builder(context)
        builer.setSingleChoiceItems(items, -1
        ) { dialog, which ->
            val commandListDialog = CommandListDialog(context, COMMAND_TYPES[which], callBack)
            //bluetoothCommandController.sendCommand(context, COMMAND_TYPES[which])
            dialog.dismiss()
            commandListDialog.showListDialog()
        }
        builer.create().show()
    }

    class CommandListDialog(private var context: Context,  private var commandType: Int, private val callBack:(command: ScannerCommand)->Unit)
    {
        private var mCommandGroup: CommandGroup = CommandGroup(context, commandType)

        init {
            mCommandGroup = CommandGroup(context, commandType)
        }

         /** 所包含的发送单元 */
        //protected var mAllScannerCommandList = ArrayList<ScannerCommand>()

        /** 列表名称 */
        var listName: String? = null

        /** 是否显示发送命令*/
        var isShowCommand: Boolean = false


        fun showListDialog() {
            // 添加所有的单元
            mCommandGroup.addAllCommands()
            /*
            for (command in mCommandGroup!!.commandList) {
                mAllScannerCommandList.add(command)
            }
            */
            // 设置选择对话框
            buildSelectDialog()
        }

        /**
         * 设置显示对话框
         */
        private fun buildSelectDialog() {
            val dialog = object : BaseDialog(context) {
                override fun setBuilder(builder: Builder) {
                    //初始化当前列表的所有数据
                    val items = arrayOfNulls<CharSequence>(
                        mCommandGroup.commandList
                        .size)
                    for (x in mCommandGroup.commandList.indices) {
                        items[x] = mCommandGroup.commandList[x].displayName
                        if (isShowCommand) {
                            items[x] = items[x].toString() + mCommandGroup.commandList[x].commandText
                        }

                    }
                    // 设置标题
                    builder.setTitle(listName)
                    // 设置选择内容
                    builder.setSingleChoiceItems(items, -1
                    ) { dialog, which ->
                        callBack.invoke(mCommandGroup.commandList[which])
                        //alex20190221.en
                        dialog.cancel()
                    }
                }

            }
            dialog.initDialog()
            dialog.show()
        }
    }
}
