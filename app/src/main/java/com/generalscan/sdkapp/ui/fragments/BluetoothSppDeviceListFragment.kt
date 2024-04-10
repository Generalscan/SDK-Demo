package com.generalscan.sdkapp.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
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
import com.generalscan.scannersdk.core.session.bluetooth.utils.BluetoothUtils
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.databinding.DeviceListBinding
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import com.generalscan.sdkapp.support.kotlinext.parcelable
import com.generalscan.sdkapp.support.utils.AppLogUtils
import com.generalscan.sdkapp.support.utils.MessageBox
import java.util.ArrayList

@SuppressLint("MissingPermission")
class BluetoothSppDeviceListFragment : Fragment() {
    private var _binding: DeviceListBinding? = null
    private val binding get() = _binding!!
    //region bluetooth

    private val list = ArrayList<BluetoothDevice>()
    private lateinit var listAdapter: DeviceListAdapter

    // Member fields
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var mScanning = false
    private var mToolbar: Toolbar? = null

    // The on-click listener for all devices in the ListViews

    private val mDeviceClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
        val device = list[position]
        // Cancel discovery because it's costly and we're about to connect
        if (bluetoothAdapter!!.isDiscovering)
            bluetoothAdapter!!.cancelDiscovery()

        val intent = Intent()
        intent.putExtra("Address", device.address)
        intent.putExtra("DeviceType", "SPP")
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private val mReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                var device = intent.parcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !list.contains(device)) {
                    var deviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME).ifNullTrim()
                    if (deviceName.isBlank()) {
                        deviceName = device.name.ifNullTrim()
                        if (deviceName.isBlank()) {
                            var remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                            if (remoteDevice != null && !remoteDevice.name.isNullOrBlank()) {
                                deviceName = remoteDevice.name.ifNullTrim()
                                device = remoteDevice
                            }
                        }
                    }
                    BluetoothUtils.removeBond(device)
                    val isLE = device.type == BluetoothDevice.DEVICE_TYPE_LE//|| device.type == BluetoothDevice.DEVICE_TYPE_DUAL

                    if(device.address.uppercase().endsWith("6D:94"))
                    {
                        val i = device.name.ifNullTrim()
                        AppLogUtils.logInfo(i)
                    }
                    if (!isLE
                        &&
                        (deviceName.startsWith("GS-")
                                || device.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.PERIPHERAL
                                || device.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.UNCATEGORIZED
                                )
                    ) {
                        if (deviceName.isNullOrBlank())
                            list.add(device)
                        else {
                            var firstEmptyNameIndex = list.indexOfFirst { it.name.isNullOrBlank() }
                            if(firstEmptyNameIndex<0)
                                firstEmptyNameIndex = 0
                            list.add(firstEmptyNameIndex, device)
                        }
                    }
                    listAdapter.notifyDataSetChanged()
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                var strSelectDevice: String? = requireActivity().intent.getStringExtra("select_device")
                if (strSelectDevice == null)
                    strSelectDevice = "Select a device to connect"
                requireActivity().title = strSelectDevice
                mScanning = false
                binding.buttonScan.setText(R.string.start_scan)
                binding.progressBar.isVisible = false
            }
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
        scanDevices()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter!!.cancelDiscovery()
        }

        // Unregister broadcast listeners
        requireActivity().unregisterReceiver(mReceiver)
    }

    @SuppressLint("NewApi")
    private fun scanDevices() {
        val strBluetoothDevices = getString(R.string.bluetooth_devices)
        requireActivity().title = strBluetoothDevices

        // Set result CANCELED in case the user backs out
        requireActivity().setResult(Activity.RESULT_CANCELED)

        // Initialize the button to perform device discovery


        binding.buttonScan.setOnClickListener {
            if (mScanning) {
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

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        requireActivity().registerReceiver(mReceiver, filter)

        // Get the local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
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
        if (D) Log.d(TAG, "doDiscovery()")
        binding.progressBar.visibility = View.VISIBLE
        mScanning = true

        // Indicate scanning in the title
        var strScanning: String? = requireActivity().intent.getStringExtra("scanning")
        if (strScanning == null)
            strScanning = "Scanning for devices..."
        //setProgressBarIndeterminateVisibility(true);
        binding.buttonScan.setText(R.string.stop_scan)
        requireActivity().title = strScanning

        // Turn on sub-title for new devices
        // findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        // If we're already discovering, stop it
        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter!!.startDiscovery()

        //if(isBluetoothLeSupported)
        //    mBluetoothLeAdapter.startLeScan(mLeScanCallback);
    }

    @SuppressLint("NewApi")
    private fun stopDiscovery() {
        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }
        //if(isBluetoothLeSupported)
        //    mBluetoothLeAdapter.stopLeScan(mLeScanCallback);
        mScanning = false
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
                var isLE = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                        isLE = true
                        holder.deviceStatus!!.visibility = View.VISIBLE
                        holder.deviceStatus!!.setImageResource(R.drawable.ic_bluetooth_le)
                    }
                }
                holder.deviceName!!.text = device.name + if (isLE) "(LE)" else ""
                holder.deviceAddress!!.text = device.address
                if (device.name != null && (device.name.indexOf("GS-BarcodeScanner") > -1 || device.name.indexOf("Generalscan SPP Barcode Scanner") > -1)) {
                    holder.deviceIcon!!.setImageResource(R.drawable.ic_scanner)

                } else {
                    holder.deviceIcon!!.setImageResource(R.drawable.ic_bluetooth)
                }
                if (!isLE) {
                    if (device.bondState == BluetoothDevice.BOND_BONDED) {
                        holder.deviceStatus!!.visibility = View.VISIBLE
                        holder.deviceStatus!!.setImageResource(R.drawable.ic_bluetooth_status_paired)
                    } else {
                        holder.deviceStatus!!.visibility = View.GONE
                    }
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
        private val TAG = "BluetoothSPP"
        private val D = true
    }
}