package com.generalscan.sdkapp.ui.activity.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.ui.activity.base.BaseActivity
import com.generalscan.sdkapp.ui.fragments.BluetoothBleDeviceListFragment
import com.generalscan.sdkapp.ui.fragments.BluetoothSppDeviceListFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

@SuppressLint("MissingPermission")
class BluetoothDeviceListActivity : BaseActivity() {


    private lateinit var tabLayout:TabLayout
    private lateinit var viewPager:ViewPager2
    private var disableAutoConnect = false

    //public static BluetoothSocket btSocket = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_device_list)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.pager)
        disableAutoConnect = intent.getBooleanExtra(EXTRA_DISABLE_AUTO_CONNECT, false)
        mayRequestLocation()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bluetooth_device_list, menu)
        if(tabLayout.selectedTabPosition==1)
        {
            menu.findItem(R.id.menu_id_whitelist).isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            R.id.menu_id_whitelist -> {
                //configureWhitelist(this)
                val newIntent = Intent(this, BluetoothLeWhitelistSettingsActivity::class.java)
                startActivity(newIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                //判断是否需要 向用户解释，为什么要申请该权限
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


    inner class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        private val NUM_TABS = 2
        override fun getItemCount(): Int {
            return NUM_TABS
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BluetoothBleDeviceListFragment(disableAutoConnect)
                else -> BluetoothSppDeviceListFragment()
            }
        }
    }


    companion object {

        private val REQUEST_FINE_LOCATION = 0

        const val EXTRA_DISABLE_AUTO_CONNECT = "AutoConnect"
    }

}
