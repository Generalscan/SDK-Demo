package com.generalscan.sdkapp.ui.activity.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.support.utils.AppLogUtils
import com.generalscan.sdkapp.support.utils.MessageBox
import com.generalscan.sdkapp.ui.activity.base.BaseActivity
import com.generalscan.sdkapp.ui.fragments.BluetoothBleDeviceListFragment
import com.generalscan.sdkapp.ui.fragments.BluetoothSppDeviceListFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


import java.util.ArrayList

@SuppressLint("MissingPermission")
class BluetoothDeviceListActivity : BaseActivity() {


    private lateinit var tabLayout:TabLayout
    private lateinit var viewPager:ViewPager2


    //public static BluetoothSocket btSocket = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_device_list)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.pager)
        mayRequestLocation()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> true
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mayRequestLocation()
    }

    private fun mayRequestLocation() {
        if (Build.VERSION.SDK_INT >= 23) {
            val checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
                    Toast.makeText(this, R.string.ble_need, Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_FINE_LOCATION)
                return
            } else {
                proceedCreation()
            }
        } else {
            proceedCreation()
        }
    }
    private fun proceedCreation()
    {
        val words = arrayListOf(
            "BLE Devices",
            "SPP Devices"
        )
        val adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = words[position]
        }.attach()
    }


    class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        private val NUM_TABS = 2
        override fun getItemCount(): Int {
            return NUM_TABS
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BluetoothBleDeviceListFragment()
                else -> BluetoothSppDeviceListFragment()
            }
        }
    }
    companion object {

        private val REQUEST_FINE_LOCATION = 0
    }

}
