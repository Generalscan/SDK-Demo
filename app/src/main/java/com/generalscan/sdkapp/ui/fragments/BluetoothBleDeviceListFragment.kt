package com.generalscan.sdkapp.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.databinding.DeviceListBinding
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import com.generalscan.sdkapp.support.utils.AppLogUtils
import com.generalscan.sdkapp.support.utils.MessageBox
import java.util.ArrayList

@SuppressLint("MissingPermission")
class BluetoothBleDeviceListFragment :Fragment()  {
    private var _binding: DeviceListBinding? = null
    private val binding get() = _binding!!
    //region bluetooth
    private lateinit var bluetoothLeScanner :BluetoothLeScanner

    private val list = ArrayList<BluetoothDevice>()
    private lateinit var listAdapter: DeviceListAdapter
    // Member fields
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanning = false
    private var mToolbar: Toolbar? = null

    // The on-click listener for all devices in the ListViews

    private val mDeviceClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
        val device = list[position]
        // Cancel discovery because it's costly and we're about to connect
        if (bluetoothAdapter!!.isDiscovering)
            bluetoothAdapter!!.cancelDiscovery()

        val intent = Intent()
        intent.putExtra("Address", device.address)
        intent.putExtra("DeviceType", "BLE")
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if(!list.contains(result.device)) {
                /*
                if(result.device.address.uppercase().endsWith("6D:94"))
                {
                    val i = result.device.bluetoothClass.majorDeviceClass.toString()
                    AppLogUtils.logInfo(i)
                }
                 */
                val deviceName = result.device.name.ifNullTrim()
                if (deviceName.isNullOrBlank())
                    list.add(result.device)
                else {
                    var firstEmptyNameIndex = list.indexOfFirst { it.name.isNullOrBlank() }
                    if(firstEmptyNameIndex<0)
                        firstEmptyNameIndex = 0
                    list.add(firstEmptyNameIndex, result.device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            var strSelectDevice: String? = requireActivity().intent.getStringExtra("select_device")
            if (strSelectDevice == null)
                strSelectDevice = "Select a device to connect"
            requireActivity().title = strSelectDevice
            scanning = false
            binding.buttonScan.setText(R.string.start_scan)
            binding.progressBar.isVisible = false
        }
    }

    //endregion
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
        scanDevices()
    }
    override fun onDestroy() {
        super.onDestroy()


    }

    @SuppressLint("NewApi")
    private fun scanDevices() {
        val strBluetoothDevices = getString(R.string.bluetooth_devices)
        requireActivity().title = strBluetoothDevices

        // Set result CANCELED in case the user backs out
        requireActivity().setResult(Activity.RESULT_CANCELED)

        // Initialize the button to perform device discovery


         binding.buttonScan.setOnClickListener {
            if (scanning) {
                stopDiscovery()
            } else {
                initDeviceList()
                doDiscovery()
            }
        }


        listAdapter = DeviceListAdapter(requireContext())

        // Find and set up the ListView for paired devices

        binding.listDevices.adapter = listAdapter
        binding.listDevices.onItemClickListener = mDeviceClickListener

        initDeviceList()
        doDiscovery()
    }

    private fun initDeviceList() {
        list.clear()
        // If there are paired devices, add each one to the ArrayAdapter
        if (list.size > 0) {
            listAdapter.notifyDataSetChanged()
        }
    }


    // Start device discover with the BluetoothAdapter
    @SuppressLint("NewApi")
    private fun doDiscovery() {
        if(scanning)
            return
        if (D) Log.d(TAG, "doDiscovery()")
        binding.progressBar.visibility = View.VISIBLE
        // Indicate scanning in the title
        var strScanning: String? = requireActivity().intent.getStringExtra("scanning")
        if (strScanning == null)
            strScanning = "Scanning for devices..."
        //setProgressBarIndeterminateVisibility(true);
          binding.buttonScan.setText(R.string.stop_scan)
        requireActivity().title = strScanning
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            Handler(Looper.getMainLooper()).postDelayed({
                stopDiscovery()
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)
        }


    }

    @SuppressLint("NewApi")
    private fun stopDiscovery() {
        bluetoothLeScanner.stopScan(leScanCallback)
        scanning = false
        binding.buttonScan.setText(R.string.start_scan)
        binding.progressBar.visibility = View.GONE
    }

    inner class DeviceListAdapter(context: Context) : BaseAdapter() {
        private val mInflater: LayoutInflater
        private val listViewResourceId = R.layout.device_name

        init {
            mInflater = LayoutInflater.from(context)
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }


        override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
            var convertView = view
            try {
                val device = list[position]
                //Feed feed = article.getFeed(this.context);
                var holder: ViewHolder? = null
                if (convertView == null) {
                    holder = ViewHolder()
                    convertView = mInflater.inflate(listViewResourceId, null)
                    holder.deviceIcon = convertView!!
                        .findViewById(R.id.image_device_icon) as ImageView
                    holder.deviceName = convertView
                        .findViewById<View>(android.R.id.text1) as TextView
                    holder.deviceAddress = convertView
                        .findViewById<View>(android.R.id.text2) as TextView
                    holder.deviceStatus = convertView
                        .findViewById(R.id.img_device_status) as ImageView
                    convertView.tag = holder
                } else {
                    holder = convertView.tag as ViewHolder
                }
                holder.deviceStatus!!.setImageResource(R.drawable.ic_bluetooth_le)
                var deviceName = device.name.ifNullTrim()
                holder.deviceName!!.text = deviceName
                holder.deviceAddress!!.text = device.address
                if (!deviceName.isNullOrBlank() &&
                    (device.name.indexOf("GS-BarcodeScanner") > -1 || device.name.indexOf("Generalscan SPP Barcode Scanner") > -1)) {
                    holder.deviceIcon!!.setImageResource(R.drawable.ic_scanner)
                } else {
                    holder.deviceIcon!!.setImageResource(R.drawable.ic_bluetooth)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                AppLogUtils.logError("DeviceListAdapter-getView", e)
                if (convertView != null)
                    MessageBox.showWarningMessage(convertView.context, e.message.toString())
            }

            return convertView
        }

        override fun getItem(position: Int): Any {
            // TODO Auto-generated method stub
            return list[position]
        }

        private inner class ViewHolder {
            var deviceName: TextView? = null
            var deviceAddress: TextView? = null
            var deviceIcon: ImageView? = null
            var deviceStatus: ImageView? = null
        }
    }

    companion object {
        // Debugging
        private val SCAN_PERIOD: Long = 10000
        private val TAG = "BluetoothSPP"
        private val D = true
    }
}