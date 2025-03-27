package com.generalscan.sdkapp.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.generalscan.scannersdk.core.basic.consts.SdkConstants
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.databinding.DeviceListBinding
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import com.generalscan.sdkapp.support.pref.BluetoothPreferences
import com.generalscan.sdkapp.support.utils.AppLogUtils
import com.generalscan.sdkapp.support.utils.MessageBox
import java.util.ArrayList
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothBleDeviceListFragment(val disableAutoConnect: Boolean) : Fragment() {
    private var _binding: DeviceListBinding? = null
    private val binding get() = _binding!!

    //region bluetooth
    private lateinit var bluetoothLeScanner: BluetoothLeScanner


    private val deviceList = ArrayList<BluetoothDevice>()
    private lateinit var listAdapter: DeviceListAdapter

    // Member fields
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanning = false


    private val shouldAutoConnect: Boolean
        get() {
            return BluetoothPreferences.deviceWhiteList.isNotBlank() &&
                    BluetoothPreferences.isAutoConnect && !disableAutoConnect
        }
    // The on-click listener for all devices in the ListViews

    private val mDeviceClickListener =
        AdapterView.OnItemClickListener { adapterView, view, position, id ->
            try {
                if (isAdded) {
                    val device = listAdapter.getItem(position)
                    // Cancel discovery because it's costly and we're about to connect
                    stopDiscovery(false)
                    selectDevice(device)
                }
            }
            catch (e: Exception)
            {
                e.printStackTrace()
                AppLogUtils.logError("On bluetooth LE device click", e)
                MessageBox.showToastMessage(requireContext(), e.message.ifNullTrim())
            }

        }


    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (!deviceList.contains(result.device)) {
                /*
                if(result.device.address.uppercase().endsWith("6D:94"))
                {
                    val i = result.device.bluetoothClass.majorDeviceClass.toString()
                    AppLogUtils.logInfo(i)
                }
                 */
                val deviceName = result.device.name.ifNullTrim()
                //val manufacture = result.scanRecord!!.manufacturerSpecificData.toString()
                if (deviceName.isNullOrBlank())
                    deviceList.add(result.device)
                else {
                    var firstEmptyNameIndex = deviceList.indexOfFirst { it.name.isNullOrBlank() }
                    if (firstEmptyNameIndex < 0)
                        firstEmptyNameIndex = 0
                    deviceList.add(firstEmptyNameIndex, result.device)
                }

                requireActivity().runOnUiThread {
                    listAdapter.notifyDataSetChanged()
                }

                //if the current device is the last connected device, stop discovery and connect to it
                if (
                    shouldAutoConnect  && result.device.address == BluetoothPreferences.lastConenctedDeviceAddress
                ) {
                    stopDiscovery()
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true) // Enable the options menu in this fragment
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
        binding.edittextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(refreshList, 500)
            }
        })
        scanDevices()
    }

    override fun onDestroy() {
        super.onDestroy()


    }

    internal var refreshList: Runnable = Runnable {
        listAdapter.filter?.filter(binding.edittextSearch.text.toString())
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
        binding.listDevices.setOnItemLongClickListener { _, _, position, _ ->
            val whiteList =
                BluetoothPreferences.deviceWhiteList.split("\n").map { it.trim() }.toMutableList()
                    .also {
                        it.remove("")
                    }
            val device = listAdapter.getItem(position)
            var deviceName = device.name.ifNullTrim()
            if (whiteList.contains(deviceName)) {
                MessageBox.showConfirmBox(
                    requireActivity(),
                    "Remove device name from whitelist?",
                    "",
                    getString(R.string.yes),
                    { _, _ ->
                        try {
                            whiteList.remove(deviceName)
                            BluetoothPreferences.deviceWhiteList = whiteList.joinToString("\n")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            MessageBox.showWarningMessage(requireActivity(), e)
                        }

                    }, getString(R.string.no)
                )
            } else {
                MessageBox.showConfirmBox(
                    requireActivity(),
                    "Add device name to whitelist?",
                    "",
                    getString(R.string.yes),
                    { _, _ ->
                        try {
                            whiteList.add(deviceName)
                            BluetoothPreferences.deviceWhiteList = whiteList.joinToString("\n")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            MessageBox.showWarningMessage(requireActivity(), e)
                        }

                    }, getString(R.string.no)
                )
            }
            true // Return false to show the default context menu
        }

        initDeviceList()
        doDiscovery()
    }

    private fun initDeviceList() {
        deviceList.clear()
        // If there are paired devices, add each one to the ArrayAdapter
        if (deviceList.size > 0) {
            listAdapter.notifyDataSetChanged()
        }
    }


    // Start device discover with the BluetoothAdapter
    @SuppressLint("NewApi")
    private fun doDiscovery() {
        if (scanning)
            return
        initDeviceList()
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
            scanning = true
            val filters = ArrayList<ScanFilter>()
            //if has white list, the add the device name to the BLE scan filter
            val hasWhiteList = BluetoothPreferences.deviceWhiteList.isNotBlank()
            var scanPeriod = SCAN_PERIOD
            if (hasWhiteList) {
                val whiteList = BluetoothPreferences.deviceWhiteList.split("\n").map { it.trim() }
                    .toMutableList().also {
                        it.remove("")
                    }
                whiteList.forEach {
                    filters.add(
                        ScanFilter.Builder()
                            .setDeviceName(it) // Filter by device name
                            .build()
                    )
                }
                //reduce scan period to 1 second for whitelist auto connect
                if (shouldAutoConnect) {
                    scanPeriod = 1000
                } else {
                    scanPeriod = 3000
                }
            }
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            bluetoothLeScanner.startScan(filters, settings, leScanCallback)
            Handler(Looper.getMainLooper()).postDelayed({
                stopDiscovery()
            }, scanPeriod)
        }


    }

    @SuppressLint("NewApi")
    private fun stopDiscovery(checkAutoConnect: Boolean = true) {
        bluetoothLeScanner.stopScan(leScanCallback)
        scanning = false
        binding.buttonScan.setText(R.string.start_scan)
        binding.progressBar.visibility = View.GONE

        //If there is whitelist and auto connect or found the last connected device, do auto connect
        if (checkAutoConnect && shouldAutoConnect) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (deviceList.any { it.address == BluetoothPreferences.lastConenctedDeviceAddress }) {
                    selectDevice(deviceList.first())
                }
            }, 200)
        }
    }

    private fun selectDevice(device: BluetoothDevice) {
        if (isAdded) {
            val intent = Intent()
            intent.putExtra("Address", device.address)
            intent.putExtra("DeviceType", SdkConstants.BLUETOOTH_DEVICE_TYPE_BLE)
            requireActivity().setResult(Activity.RESULT_OK, intent)
            requireActivity().finish()
        }
    }

    inner class DeviceListAdapter(context: Context) : BaseAdapter(), Filterable {
        private val mInflater: LayoutInflater
        private val listViewResourceId = R.layout.view_item_device_name
        var dataList: List<BluetoothDevice>
            private set

        init {
            mInflater = LayoutInflater.from(context)
            dataList = deviceList
        }

        override fun getCount(): Int {
            return dataList.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }


        override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
            var convertView = view
            try {
                val device = dataList[position]
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
                    (device.name.indexOf("GS-BarcodeScanner") > -1 || device.name.indexOf("Generalscan SPP Barcode Scanner") > -1)
                ) {
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

        override fun getItem(position: Int): BluetoothDevice {
            // TODO Auto-generated method stub
            return dataList[position]
        }

        override fun getFilter(): Filter? {
            val filter = object : Filter() {

                override fun publishResults(
                    constraint: CharSequence,
                    results: Filter.FilterResults
                ) {
                    dataList = results.values as List<BluetoothDevice>
                    notifyDataSetChanged()
                }

                override fun performFiltering(filter: CharSequence): Filter.FilterResults {
                    var constraint = filter

                    val results = Filter.FilterResults()
                    val FilteredArrayNames = ArrayList<BluetoothDevice>()
                    constraint = constraint.ifNullTrim()
                    for (i in deviceList.indices) {
                        if (deviceList[i].name.ifNullTrim()
                                .contains(constraint, ignoreCase = true)
                        ) {
                            FilteredArrayNames.add(deviceList[i])
                        }
                    }

                    results.count = FilteredArrayNames.size
                    results.values = FilteredArrayNames
                    Log.e("VALUES", results.values.toString())
                    return results
                }
            }
            return filter
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