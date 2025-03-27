package com.generalscan.sdkapp.ui.activity.bluetooth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.generalscan.quickpair.ui.activity.base.BaseBindingActivity
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.databinding.ActivityBlueutoothLeWhitelistSettingsBinding
import com.generalscan.sdkapp.support.kotlinext.ifNullTrim
import com.generalscan.sdkapp.support.models.DeviceWhitelist
import com.generalscan.sdkapp.support.pref.BluetoothPreferences
import com.generalscan.sdkapp.support.utils.AppLogUtils
import com.generalscan.sdkapp.support.utils.MessageBox

class BluetoothLeWhitelistSettingsActivity : BaseBindingActivity<ActivityBlueutoothLeWhitelistSettingsBinding>() {

    private val deviceWhitelist = ArrayList<DeviceWhitelist>()
    //private lateinit var deviceWhitelistAdapter: DeviceWhiteListAdapter

    override fun setupViewBinding(inflater: LayoutInflater) = ActivityBlueutoothLeWhitelistSettingsBinding.inflate(inflater)

    override fun beforeCreate() {
        binding.activity = this
    }

    override fun afterCreate(savedInstanceState: Bundle?) {
        try {
            supportActionBar?.elevation = 0f
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeButtonEnabled(true)
            if(BluetoothPreferences.deviceWhiteList.isNotBlank()) {
                val whiteList =
                    BluetoothPreferences.deviceWhiteList.split(",").map { it.trim() }.toMutableList()
                        .also {
                            it.remove("")
                        }
                for (item in whiteList)
                {
                    deviceWhitelist.add(DeviceWhitelist().also {
                        it.deviceName = item
                    })
                }
            }
            binding.checkboxAutoConnect.isChecked = BluetoothPreferences.isAutoConnect
            //deviceWhitelistAdapter = DeviceWhiteListAdapter(this)
            binding.listviewDeviceWhitelist.adapter = SimpleStringRecyclerViewAdapter(this)
        } catch (e: Exception) {
            e.printStackTrace()
            showWarningMessage(e.message)
        }
    }

    //region whitelist

    fun cancel() {
        this.finish()
    }
    fun saveSettings(){
        BluetoothPreferences.isAutoConnect = binding.checkboxAutoConnect.isChecked
        BluetoothPreferences.deviceWhiteList = deviceWhitelist.map { it.deviceName }.joinToString("\n")
        this.finish()
    }


    private fun deleteWhitelist(deviceName: String){
        deviceWhitelist.removeAll { it.deviceName == deviceName }
        binding.listviewDeviceWhitelist.adapter?.notifyDataSetChanged()
    }
    private inner class SimpleStringRecyclerViewAdapter(context: Context) :
        RecyclerView.Adapter<SimpleStringRecyclerViewAdapter.ViewHolder>() {


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

            var textViewDeviceName: TextView = view.findViewById(R.id.textview_device_name)!!
            var imageViewDelete: ImageButton = view.findViewById(R.id.imagebutton_delete)
            init {
                imageViewDelete.setOnClickListener(this)
            }
            override fun onClick(view: View) {
                try {
                    val position = Integer.valueOf(view.tag.toString())
                    val entity = deviceWhitelist[position]
                    if (view.id == R.id.imagebutton_delete) {
                        deleteWhitelist(entity.deviceName)
                        return
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    AppLogUtils.logError("AddUserIssue", e)
                    MessageBox.showWarningMessage(this@BluetoothLeWhitelistSettingsActivity, e.message.ifNullTrim())
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_item_device_whitelist, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entity = deviceWhitelist[position]
            holder.textViewDeviceName.text = entity.deviceName
            holder.imageViewDelete.tag = position
        }

        override fun getItemCount(): Int = deviceWhitelist.size
    }
    //endregion
    companion object {

    }
}