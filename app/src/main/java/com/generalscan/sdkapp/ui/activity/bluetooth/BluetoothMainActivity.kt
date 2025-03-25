package com.generalscan.sdkapp.ui.activity.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatEditText
import com.generalscan.scannersdk.core.basic.SdkContext
import com.generalscan.scannersdk.core.basic.consts.SdkConstants
import com.generalscan.scannersdk.core.basic.interfaces.BluetoothPairListener
import com.generalscan.scannersdk.core.basic.interfaces.CommunicateListener
import com.generalscan.scannersdk.core.basic.interfaces.IConnectSession
import com.generalscan.scannersdk.core.basic.interfaces.SessionListener
import com.generalscan.scannersdk.core.session.bluetooth.BluetoothSettings
import com.generalscan.scannersdk.core.session.bluetooth.connect.BluetoothConnectSession
import com.generalscan.scannersdk.core.session.bluetooth.utils.BluetoothPairCtl
import com.generalscan.scannersdk.core.session.bluetooth.utils.BluetoothUtils
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.support.inject.ViewInject
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import com.generalscan.sdkapp.support.kotlinext.textTrim
import com.generalscan.sdkapp.support.pref.BluetoothPreferences
import com.generalscan.sdkapp.support.utils.AppLogUtils
import com.generalscan.sdkapp.support.utils.LeUtils
import com.generalscan.sdkapp.support.utils.MessageBox
import com.generalscan.sdkapp.support.utils.PermissionUtils
import com.generalscan.sdkapp.system.base.AppContext
import com.generalscan.sdkapp.ui.activity.base.BaseBluetoothActivity
import com.generalscan.sdkapp.ui.widgets.BluetoothCommandListDialog


class BluetoothMainActivity : BaseBluetoothActivity() {
    val REQUEST_SELECT_BLUETOOTH_DEVICE = 10
    private val REQUEST_SCANNING_REQUEST_CODE = 20
    private val REQUEST_BLUETOOTH_SETTINGS = 30
    private val REQUEST_CONNECT_BLUETOOTH_DEVICE = 50

    private var pairController: BluetoothPairCtl? = null
    private lateinit var bluetoothConnectSession: BluetoothConnectSession
    private var showLineFeed = false

    private var selectedDeviceAddress: String = ""
    private var selectedDeviceType: String = ""

    //region view injection
    @ViewInject(id = R.id.btnTurnBluetooth)
    private lateinit var btnTurnBluetooth: TextView

    @ViewInject(id = R.id.btnConfigureWhiteList)
    private lateinit var btnConfigureWhiteList: TextView

    @ViewInject(id = R.id.btnSelectDevice)
    private lateinit var btnSelectDevice: TextView

    @ViewInject(id = R.id.layConnect)
    private lateinit var mLayConnect: ViewGroup

    @ViewInject(id = R.id.laySetting)
    private lateinit var mLaySetting: ViewGroup

    @ViewInject(id = R.id.layRawData)
    private lateinit var mLayRawData: ViewGroup

    // 发送指定内容
    @ViewInject(id = R.id.btnSendContent)
    private lateinit var myBtnSendContent: Button
    // 清空

    @ViewInject(id = R.id.txtReceiveData)
    private lateinit var mTxtReceiveData: EditText

    @ViewInject(id = R.id.txtRawData)
    private lateinit var mTxtRawData: EditText

    @ViewInject(id = R.id.txtCommand)
    private lateinit var mTxtCommand: EditText

    @ViewInject(id = R.id.chkShowSpecialFeed)
    private lateinit var mChkShowSpecialFeed: CheckBox

    @ViewInject(id = R.id.chkShowRawData)
    private lateinit var mChkShowRawData: CheckBox

    @ViewInject(id = R.id.chkOldFireware)
    private lateinit var mChkOldFirmware: CheckBox
    //endregion
    //region override methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_main)
        proceedActivityCreation()
        SdkContext.performInsecureSppConnection = false
        mChkOldFirmware.setOnCheckedChangeListener { buttonView, isChecked ->
            SdkContext.isOldFirmware = isChecked
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECT_BLUETOOTH_DEVICE -> {
                if (resultCode == Activity.RESULT_OK) {
                    selectedDeviceAddress = data?.getStringExtra("Address").ifNullTrim()
                    selectedDeviceType = data?.getStringExtra("DeviceType").ifNullTrim()
                    if (SdkContext.performInsecureSppConnection)
                        connectDevice()
                    else
                        pairDevice(false)
                }
                else
                {
                    btnSelectDevice.isEnabled = true
                }
            }
            REQUEST_SCANNING_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data?.extras
                selectedDeviceAddress = bundle!!.getString("result").ifNullTrim()
                if(SdkContext.performInsecureSppConnection)
                    connectDevice()
                else
                    pairDevice(true)
            }
            REQUEST_BLUETOOTH_SETTINGS -> if (resultCode == Activity.RESULT_OK) {
                updateBluetoothStatus()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            //if(bluetoothConnectSession.isConnected)
            pairController?.releaseRegister()
            bluetoothConnectSession.endSession();
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bluetooth_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_id_clear_output -> {
                mTxtReceiveData.text.clear()
                mTxtRawData.text.clear()
            }
            R.id.menu_id_command_list ->{
                val commandListDialog = BluetoothCommandListDialog(this, bluetoothConnectSession.commandController)
                commandListDialog.ShowoDialog()
            }
        }

        return super.onOptionsItemSelected(item)
    }
    //endregion

    //region helper functions


    private fun pairDevice(deviceDiscovery: Boolean) {
        if (pairController == null) {
            pairController = BluetoothPairCtl(this, PairListener());
        }
        AppLogUtils.sysLog("Start to pair device")
        //We do provide PIN because User may change PIN in some of the cases
        pairController!!.tryPairDevice(selectedDeviceAddress!!, "")
    }

    private fun connectDevice() {
        AppLogUtils.sysLog("Start to connect device")

        if(bluetoothConnectSession.isConnected)
        {
            bluetoothConnectSession.disconnect()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            val device = mBluetoothAdapter!!.getRemoteDevice(selectedDeviceAddress)
            bluetoothConnectSession.bluetoothDeviceToConnect = device
            bluetoothConnectSession.deviceType = selectedDeviceType
            bluetoothConnectSession.connect()
        }, 500)
    }

    ///private fun
    private fun initBluetooth() {
        bluetoothConnectSession = BluetoothConnectSession(this)
        bluetoothConnectSession.sessionListener =object : SessionListener{
            override fun onSessionReady(session: IConnectSession) {
                bluetoothConnectSession.setConnectListener(
                        object : CommunicateListener {

                            override fun onConnected() {
                                MessageBox.showToastMessage(this@BluetoothMainActivity, R.string.scanner_connect_success)
                                btnSelectDevice.isEnabled = false
                                mLayConnect.visibility = View.GONE
                                mLaySetting.visibility = View.VISIBLE
                                if(selectedDeviceType == SdkConstants.BLUETOOTH_DEVICE_TYPE_BLE)
                                {
                                    BluetoothPreferences.lastConenctedDeviceAddress = selectedDeviceAddress
                                }
                            }

                            override fun onConnectFailure(errorMessage: String) {
                                btnSelectDevice.isEnabled = true
                                try {
                                    MessageBox.showWarningMessage(this@BluetoothMainActivity, errorMessage)
                                } catch (e: Exception) {
                                    MessageBox.showToastMessage(AppContext.instance, errorMessage)
                                }

                            }

                            override fun onRawDataReceived(data: Byte) {
                                if(mChkShowRawData.isChecked) {
                                    var output = ""
                                    when (data.toInt()) {
                                        0x04 -> output = "{EOT}"
                                        0x0d -> output = "{CR}"
                                        0x0a -> output = "{LF}"
                                        else -> {
                                            mTxtRawData.append("0x")
                                            mTxtRawData.append(String.format("%02X", data) + " ")
                                            output = data.toChar().toString()
                                        }
                                    }
                                    mTxtRawData.append(output)
                                    mTxtRawData.append("\r\n")
                                    val scrollView = (mTxtRawData.parent as ScrollView)
                                    scrollView.post({ scrollView.fullScroll(View.FOCUS_DOWN) })
                                }
                            }

                            override fun onDisconnected() {
                                MessageBox.showToastMessage(this@BluetoothMainActivity, "Bluetooth has been disconnected")
                            }

                            override fun onBatteryDataReceived(voltage: String, percentage: String) {
                                mTxtReceiveData.append(voltage + ":" + percentage);
                            }

                            override fun onDataReceived(data: String) {
                                if (mChkShowSpecialFeed.isChecked) {
                                    var displayData = data.replace("\n", "{LF}")
                                    displayData = displayData.replace("\r", "{CR}");
                                    displayData = displayData.replace("\u0004", "{EOT}");
                                    mTxtReceiveData.append(displayData)
                                } else {
                                    mTxtReceiveData.append(data)
                                }
                                val scrollView = (mTxtReceiveData.parent as ScrollView)
                                scrollView.post({ scrollView.fullScroll(View.FOCUS_DOWN) })
                            }

                            override fun onCommandCallback(name: String, data: String) {
                                mTxtReceiveData.append("$name:$data");
                                val scrollView = (mTxtReceiveData.parent as ScrollView)
                                scrollView.post({ scrollView.fullScroll(View.FOCUS_DOWN) })
                            }

                            override fun onCommandNoResponse(errorMessage: String) {

                            }
                        }
                )
            }

            override fun onSessionStartTimeOut(session: IConnectSession) {
                MessageBox.showWarningMessage(this@BluetoothMainActivity, "Session Timeout")
            }
        };

        bluetoothConnectSession.startSession()

    }

    private fun updateBluetoothStatus() {

        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }
        if (mBluetoothAdapter!!.isEnabled) {
            btnTurnBluetooth.isEnabled = false
            btnTurnBluetooth.setText(R.string.B_HadTurnOn)
        }


    }

    @SuppressLint("MissingPermission")
    private fun setListener() {
        btnTurnBluetooth.setOnClickListener {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_SETTINGS)
        }
        btnConfigureWhiteList.setOnClickListener {
            LeUtils.configureWhitelist(this)
        }
        btnSelectDevice.setOnClickListener {
            btnSelectDevice.isEnabled = false
            val intent = Intent(this, BluetoothDeviceListActivity::class.java)
            startActivityForResult(intent, REQUEST_SELECT_BLUETOOTH_DEVICE)
        }


        // 发送命令
        // Send command
        myBtnSendContent.setOnClickListener({

            // 获取输入的命令
            val text = mTxtCommand.text.toString()
            mTxtReceiveData.requestFocus()
            bluetoothConnectSession.sendData(text)


        })


        mChkShowSpecialFeed.setOnCheckedChangeListener { compoundButton, checked ->
            showLineFeed = checked
        }

        mChkShowRawData.setOnCheckedChangeListener{
            _, checked ->
            if(checked)
                mLayRawData.visibility = View.VISIBLE
            else
                mLayRawData.visibility = View.GONE

        }
    }

    inner class PairListener : BluetoothPairListener {
        override fun onRequestPin() {

        }

        override fun onPairSuccess(isBle: Boolean) {
            AppLogUtils.sysLog("Pair device completed")
            BluetoothSettings.setSelectedScanner(selectedDeviceAddress!!)
            connectDevice()
        }

        override fun onFailure(errorMessage: String) {
            btnSelectDevice.isEnabled = true
            MessageBox.showWarningMessage(this@BluetoothMainActivity, errorMessage)
        }

    }

    //endregion



    private fun proceedActivityCreation() {
        mLayConnect.visibility = View.VISIBLE
        mLaySetting.visibility = View.GONE
        initBluetooth()
        updateBluetoothStatus()
        setListener()
    }


}
