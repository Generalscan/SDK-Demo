package com.generalscan.sdkapp.ui.activity.usb

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import cn.jimex.fms.support.utils.SdkLogUtils
import com.generalscan.quickpair.ui.activity.base.BaseBindingActivity
import com.generalscan.scannersdk.core.session.usbhost.connect.usbserial.GeneralScanUsbDevice
import com.generalscan.scannersdk.core.session.usbhost.connect.usbserial.UsbSerialDevice
import com.generalscan.scannersdk.core.session.usbhost.connect.usbserial.UsbSerialInterface
import com.generalscan.scannersdk.core.session.usbhost.service.UsbHostService
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.databinding.ActivityUsbSerialMainBinding
import com.generalscan.sdkapp.support.inject.InjectUtility
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import com.generalscan.sdkapp.support.kotlinext.textTrim
import com.generalscan.sdkapp.support.models.GsUsbDeviceModel
import com.generalscan.sdkapp.support.utils.AppLogUtils
import com.generalscan.sdkapp.support.utils.MessageBox
import com.generalscan.sdkapp.ui.widgets.BluetoothCommandListDialog
import com.generalscan.sdkapp.ui.widgets.UsbDeviceCommandListDialog
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class UsbSerialMainActivity : BaseBindingActivity<ActivityUsbSerialMainBinding>() {

    private lateinit var usbManager: UsbManager
    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var serialPort: UsbSerialDevice? = null
    private var serialPortConnected: Boolean = false
    private var needRequestPermission: Boolean = false //alex20210118.n
    private lateinit var handler: Handler
    private var connectionThread:ConnectionThread? = null
    private var isInit = false
    private var isDestroying = false
    private var deviceModels = listOf(
        GsUsbDeviceModel().apply{
            manufacturerName = "GENERALSCAN"
            vendorId = 1155
            productId = 22336
            scannerModel = "RX2X5X"
        },
        GsUsbDeviceModel().apply{
            manufacturerName = "TMC"
            vendorId = 9969
            productId = 34818
            scannerModel = "WT1521"
        }
    )
    override fun setupViewBinding(inflater: LayoutInflater) = ActivityUsbSerialMainBinding.inflate(inflater)

    override fun beforeCreate() {
        binding.activity = this
    }


    override fun afterCreate(savedInstanceState: Bundle?) {
        try {
            supportActionBar?.elevation = 0f
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeButtonEnabled(true)
            if (!isInit) {
                usbManager = getSystemService(USB_SERVICE) as UsbManager
            }
            handler = MyHandler()
            registerReceiver()
            findSerialPortDevice()
        } catch (e: Exception) {
            e.printStackTrace()
            showWarningMessage(e.message)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if(binding.layoutDataReceive.isVisible) {
            menuInflater.inflate(R.menu.menu_usb_serial_main, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_id_clear_output -> {
                binding.edittextReceiveData.text.clear()
            }
            R.id.menu_id_command_list ->{
                val commandListDialog = UsbDeviceCommandListDialog(this){
                    sendDataToDevice(it.commandText)
                }
                commandListDialog.ShowoDialog()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroying = true
        try {
            disconnectDevice()
        }
        catch(e:Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(usbReceiver)
        }
        catch(e:Exception) {
            e.printStackTrace()
        }

    }

    //region USB functions
    /*
    * Different notifications from OS will be received here (USB attached, detached, permission responses...)
    * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
    */
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, inputIntent: Intent) {
            if (inputIntent.action == ACTION_USB_PERMISSION) {
                synchronized (this) {
                    val granted = inputIntent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                    if (granted)
                    // User accepted our USB connection. Try to open the device as a serial port
                    {
                        sendMessage(MESSAGE_USB_PERMISSION_GRANTED)
                    } else
                    // User not accepted our USB connection. Send an Intent to the Main Activity
                    {
                        sendMessage(MESSAGE_USB_PERMISSION_NOT_GRANTED)
                    }
                }
            } else if (inputIntent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                sendMessage(MESSAGE_USB_ATTACHED)
            } else if (inputIntent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                sendMessage(MESSAGE_USB_DETACHED)
            }
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED)
        }
        else
        {
            registerReceiver(usbReceiver, filter)
        }
    }
    /*
    * A simple thread to open a serial port.
    * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
    */
    private inner class ConnectionThread : Thread() {
        override fun run() {
            if(device==null)
                return
            if(connection==null)
                throw Exception("No USB device connection!!")
            serialPort = UsbSerialDevice.createUsbSerialDevice(device!!, connection!!)
            if (serialPort != null) {
                if (serialPort!!.open()) {
                    serialPortConnected = true
                    serialPort!!.setBaudRate(GeneralScanUsbDevice.BAUD_RATE)
                    serialPort!!.setDataBits(GeneralScanUsbDevice.DATA_BIT)
                    serialPort!!.setStopBits(GeneralScanUsbDevice.STOP_BIT)
                    serialPort!!.setParity(GeneralScanUsbDevice.PARITY)
                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPort!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                    serialPort!!.read(mCallback)
                    serialPort!!.getCTS(ctsCallback)
                    serialPort!!.getDSR(dsrCallback)
                    // Everything went as expected. Send an intent to MainActivity
                    sendMessage(MESSAGE_USB_READY)
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort is GeneralScanUsbDevice) {
                        sendMessage(MESSAGE_CDC_DRIVER_NOT_WORKING)
                    } else {
                        sendMessage(MESSAGE_USB_DEVICE_NOT_WORKING)
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                sendMessage(MESSAGE_USB_NOT_SUPPORTED)
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun findSerialPortDevice() {
        try {
            binding.buttonConnect.isVisible = false
            needRequestPermission = false //alex20210118.n
            var foundDevice = false
            // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
            var usbDevices = usbManager.deviceList
            if (!usbDevices.isEmpty()) {
                var keep = true

                for ((_, value) in usbDevices) {
                    device = value
                    if(device==null)
                        continue
                    //val deviceVID = device!!.vendorId
                    //val devicePID = device!!.productId
                    val manufactureName =  device!!.manufacturerName.ifNullTrim()
                    binding.textviewDeviceManufacturer.text = manufactureName
                    binding.textviewDeviceVendorId.text = device!!.vendorId.toString()
                    binding.textviewDeviceProductId.text = device!!.productId.toString()
                    var deviceModel = deviceModels.firstOrNull() { it.vendorId == device!!.vendorId && it.productId == device!!.productId
                            && it.manufacturerName == manufactureName}
                    if(deviceModel != null) {
                        //binding.textviewDeviceName.text = device!!.productName
                        binding.textviewDeviceModel.setTextColor(ContextCompat.getColor(this, R.color.text_100))
                        binding.textviewDeviceModel.text = deviceModel.scannerModel
                        binding.layoutDeviceManufacturer.isVisible = false
                        binding.layoutDeviceVendorId.isVisible = false
                        binding.layoutDeviceProductId.isVisible = false
                        binding.buttonConnect.isEnabled = true
                        keep = false
                        foundDevice = true
                    }
                    else
                    {
                        binding.layoutDeviceManufacturer.isVisible = true
                        binding.layoutDeviceVendorId.isVisible = true
                        binding.layoutDeviceProductId.isVisible = true
                        AppLogUtils.logInfo("Device with vendor id(${device!!.vendorId}),product id(${device!!.productId}) and MFR(${manufactureName}) is not supported")
                        binding.textviewDeviceModel.setTextColor(ContextCompat.getColor(this, R.color.error))
                        binding.textviewDeviceModel.text = "Device is not supported"
                        connection = null
                        device = null
                    }
                    /*
                    if (deviceVID != 0x1d6b && devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003 && deviceVID != 0x5c6 && devicePID != 0x904c) {
                        binding.buttonConnect.isEnabled = true
                        keep = false
                    } else {
                        binding.textviewDeviceName.append(" (Not supported)")
                        connection = null
                        device = null
                    }
                     */
                    if (!keep)
                        break
                }
                if (!keep) {
                    // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                    //val intent = Intent(UsbHostService.Companion.ACTION_NO_USB)
                    //sendBroadcast(intent)
                }
            } else {
                // There is no USB devices connected. Send an intent to MainActivity
                //val intent = Intent(UsbHostService.Companion.ACTION_NO_USB)
                //sendBroadcast(intent)
            }
            binding.buttonConnect.isVisible = foundDevice
        }
        catch (e: Throwable)
        {
            e.printStackTrace()
            MessageBox.showWarningMessage(this, e.message.ifNullTrim())
        }

    }

    fun onConnectButtonClick()
    {
        // There is a device connected to our Android device. Try to open it as a Serial Port.
        //alex20210118.sn
        if(!usbManager.hasPermission(device)) {
            needRequestPermission = true
            //Request user permission
            var intent = Intent(ACTION_USB_PERMISSION);
            intent.setPackage(packageName);
            val mPendingIntent = PendingIntent.getBroadcast(this, 0, intent,  PendingIntent.FLAG_MUTABLE)
            usbManager.requestPermission(device, mPendingIntent)
        }
        else
        {
            connectDevice()
        }
        //alex20210118.en
    }

    fun onSendButtonClick(){
        sendDataToDevice(binding.edittextCommand.textTrim)
    }
    private fun connectDevice()
    {
        connection = usbManager.openDevice(device)
        connectionThread = ConnectionThread()
        connectionThread?.isDaemon = true//exit when main thread exit
        connectionThread?.start()
    }

    private fun disconnectDevice(){
        try {
            if (serialPortConnected) {
                serialPort!!.close()
            }
            serialPortConnected = false
            if(!isDestroying) {
                binding.buttonConnect.isEnabled = false
                binding.buttonSendCommand.isEnabled = false
                binding.layoutConnect.isVisible = true
                binding.layoutDataReceive.isVisible = false
            }
            connectionThread?.interrupt()
            invalidateOptionsMenu()
        } catch (e: Throwable) {
            e.printStackTrace()
            MessageBox.showToastMessage(this, e.message.toString())
        }
    }
    fun sendDataToDevice(text: String) {
        sendDataToDevice(text.toByteArray());
    }

    fun sendDataToDevice(bytes: ByteArray) {
        try {
            if (serialPort != null) {
                serialPort!!.write(bytes)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            MessageBox.showToastMessage(this, e.message.toString())
        }
    }

    //region callbacks
    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */
    private val mCallback = object : UsbSerialInterface.UsbReadCallback {
        override fun onReceivedData(receivedBytes: ByteArray?) {
            try {
                if(receivedBytes!=null) {
                    sendMessage(MESSAGE_DATA_RECEIVED, receivedBytes)
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

        }
    }

    /*
     * State changes in the CTS line will be received here
     */
    private val ctsCallback = object : UsbSerialInterface.UsbCTSCallback {
        override fun onCTSChanged(state: Boolean) {
            //if (mHandler != null)
            //   mHandler!!.obtainMessage(CTS_CHANGE).sendToTarget()
        }
    }

    /*
     * State changes in the DSR line will be received here
     */
    private val dsrCallback = object : UsbSerialInterface.UsbDSRCallback {
        override fun onDSRChanged(state: Boolean) {
            //if (mHandler != null)
            //    mHandler!!.obtainMessage(DSR_CHANGE).sendToTarget()
        }
    }
    //endregion

    //endregion
    //region handler
    @SuppressLint("HandlerLeak")
    inner class MyHandler internal constructor() : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if(isDestroying)
                return
            when (msg.what) {
                MESSAGE_USB_ATTACHED->{
                    if (!serialPortConnected) {
                        findSerialPortDevice()
                    }
                }
                MESSAGE_USB_DETACHED->{
                    disconnectDevice()
                }
                MESSAGE_USB_PERMISSION_GRANTED->{
                    connectDevice()
                }
                MESSAGE_USB_PERMISSION_NOT_GRANTED->{
                    binding.buttonSendCommand.isEnabled = false
                }
                MESSAGE_USB_READY -> {
                    binding.buttonSendCommand.isEnabled = true
                    binding.layoutConnect.isVisible = false
                    binding.layoutDataReceive.isVisible = true
                    invalidateOptionsMenu()
                }

                MESSAGE_DATA_RECEIVED -> {
                    var receivedBytes = msg.obj as ByteArray
                    if(binding.radiobuttonHex.isChecked) {
                        var output = ""
                        for(data in receivedBytes) {
                            when (data.toInt()) {
                                0x04 -> output = "{EOT}"
                                0x0d -> output = "{CR}"
                                0x0a -> output = "{LF}"
                                else -> {
                                    binding.edittextReceiveData.append("0x")
                                    binding.edittextReceiveData.append(String.format("%02X", data) + " ")
                                    output = data.toInt().toChar().toString()
                                }
                            }
                            binding.edittextReceiveData.append(output)
                            binding.edittextReceiveData.append("\r\n")
                            val scrollView = (binding.edittextReceiveData.parent as ScrollView)
                            scrollView.post({ scrollView.fullScroll(View.FOCUS_DOWN) })
                        }
                    }
                    else
                    {
                        val dataString = String(receivedBytes, Charset.forName("UTF-8"))
                        AppLogUtils.sysLog("Received USB Data:${dataString}")
                        binding.edittextReceiveData.append(dataString)
                    }
                }

                MESSAGE_CDC_DRIVER_NOT_WORKING -> {
                    MessageBox.showToastMessage(this@UsbSerialMainActivity, "CDC Driver is not working")
                }

                MESSAGE_USB_DEVICE_NOT_WORKING -> {
                    MessageBox.showToastMessage(this@UsbSerialMainActivity, "USB device is not working")
                }
                MESSAGE_USB_NOT_SUPPORTED -> {
                    MessageBox.showToastMessage(this@UsbSerialMainActivity, "No driver for this device")
                }
            }
        }
    }
    private fun sendMessage(messageId: Int, obj: Any? = null) {
        val msg = Message()
        msg.what = messageId
        msg.obj = obj
        handler.sendMessage(msg)
    }
    //endregion
    companion object {
        private const val MESSAGE_USB_ATTACHED = 1
        private const val MESSAGE_USB_DETACHED = 2

        private const val MESSAGE_USB_READY = 10
        private const val MESSAGE_CDC_DRIVER_NOT_WORKING = 11
        private const val MESSAGE_USB_DEVICE_NOT_WORKING = 32
        private const val MESSAGE_USB_NOT_SUPPORTED = 13

        private const val MESSAGE_USB_PERMISSION_GRANTED = 100
        private const val MESSAGE_USB_PERMISSION_NOT_GRANTED = 101
        private const val MESSAGE_DATA_RECEIVED = 200
        private const val TAG = "MAIN_ACTIVITY"
        private const val REQUEST_USER_PERMISSION = 30


        //val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        //val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        val ACTION_USB_NOT_SUPPORTED = "com.generalscan.usbservice.USB_NOT_SUPPORTED"
        val ACTION_NO_USB = "com.generalscan.usbservice.NO_USB"

        //val ACTION_USB_DISCONNECTED = "com.generalscan.usbservice.USB_DISCONNECTED"
        //val ACTION_CDC_DRIVER_NOT_WORKING = "com.generalscan.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING"
        //val ACTION_USB_DEVICE_NOT_WORKING = "com.generalscan.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING"

        private val ACTION_USB_PERMISSION = "com.generalscan.sdkapp.USB_PERMISSION"
    }
}