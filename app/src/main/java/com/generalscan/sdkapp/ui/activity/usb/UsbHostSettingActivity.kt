package com.generalscan.sdkapp.ui.activity.usb

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import com.generalscan.scannersdk.core.basic.interfaces.CommunicateListener
import com.generalscan.scannersdk.core.pref.UsbHostReferences
import com.generalscan.scannersdk.core.session.usbhost.basic.UsbHostConsts
import com.generalscan.scannersdk.core.session.usbhost.connect.UsbHostConnectSession
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.support.inject.ViewInject
import com.generalscan.sdkapp.support.kotlinext.ifNullOrBlank
import com.generalscan.sdkapp.support.task.AsTask
import com.generalscan.sdkapp.support.task.CallResult
import com.generalscan.sdkapp.support.utils.MessageBox
import com.generalscan.sdkapp.ui.activity.base.BaseActivity

class UsbHostSettingActivity : BaseActivity(),  CompoundButton.OnCheckedChangeListener {
    val TEXT_SIZE = 10

    //region view inject
    @ViewInject(id = R.id.ckbVibrate)
    private lateinit var mCkbVibrate: CheckBox

    @ViewInject(id = R.id.edtVibrateTime)
    private lateinit var mEdtVibrateTime: EditText

    @ViewInject(id = R.id.ckbtouch)
    private lateinit var mckbtouch: CheckBox

    @ViewInject(id = R.id.edtTime)
    private lateinit var mEdtTime: EditText

    @ViewInject(id = R.id.btnSave)
    private lateinit var mBtnSave: TextView

    @ViewInject(id = R.id.btn_show)
    private lateinit var btn_show: Button

    @ViewInject(id = R.id.btn_hide)
    private lateinit var btn_hide: Button

    @ViewInject(id = R.id.seekBar)
    private lateinit var mSeekBar: SeekBar// 透明度调整

    @ViewInject(id = R.id.seekBarFont)
    private lateinit var mSeekBarFont: SeekBar// 字体大小

    @ViewInject(id = R.id.tvFont)
    private lateinit var mTvFont: TextView// 测试字体

    @ViewInject(id = R.id.radio0)
    private lateinit var mRbtn1: RadioButton// 样式选择
    @ViewInject(id = R.id.radio1)
    private lateinit var mRbtn2: RadioButton
    @ViewInject(id = R.id.radio2)
    private lateinit var mRbtn3: RadioButton

    @ViewInject(id = R.id.radioTriggerMethods)
    private lateinit var radioTriggerMethods: RadioGroup
    @ViewInject(id = R.id.radioTriggerByGravity)
    private lateinit var mRadioTriggerByGravity: RadioButton
    @ViewInject(id = R.id.radioTriggerByButton)
    private lateinit var mRadioTriggerByButton: RadioButton

    @ViewInject(id = R.id.tvData)
    private lateinit var mTvData: TextView

    @ViewInject(id = R.id.layTriggerButtonSettings)
    private lateinit var mLayTriggerButtonSettings: View
    //endregion

    private var serviceIsOn = false

    private lateinit var mConnectionSession:UsbHostConnectSession


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_host)

        mLayTriggerButtonSettings.visibility = View.GONE
        mConnectionSession = UsbHostConnectSession(this, false)

        setListeners()

        readSettings()

        saveAndRestartService()

    }
    override fun onDestroy() {
        super.onDestroy()
        if (mConnectionSession.floatingScanButtonService != null) {
            mConnectionSession.endSession()
        }
    }
    override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
        if (mConnectionSession.floatingScanButtonService != null) {

            if (mRbtn1.isChecked) {
                // 设置透明度
                mConnectionSession.floatingScanButtonService?.setBtnBg(1)
                UsbHostReferences.triggerButtonBackground =  1
            } else if (mRbtn2.isChecked) {
                // 设置透明度
                mConnectionSession.floatingScanButtonService?.setBtnBg(2)
                UsbHostReferences.triggerButtonBackground = 2
            } else if (mRbtn3.isChecked) {
                // 设置透明度
                mConnectionSession.floatingScanButtonService?.setBtnBg(3)
                UsbHostReferences.triggerButtonBackground =  3
            }
        }
    }

    private fun setListeners() {

        btn_show.setOnClickListener {
            mConnectionSession.startSession()
        }
        btn_hide.setOnClickListener {
            mConnectionSession.endSession()
        }

        mBtnSave.setOnClickListener {
            saveSettings()
            MessageBox.showWarningMessage(this, R.string.success)
        }
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(arg0: SeekBar) {
                // TODO Auto-generated method stub

            }

            override fun onStartTrackingTouch(arg0: SeekBar) {
                // TODO Auto-generated method stub

            }

            override fun onProgressChanged(arg0: SeekBar, arg1: Int, arg2: Boolean) {
                // 连接Usb
                if (mConnectionSession.floatingScanButtonService != null) {
                    // 设置透明度
                    mConnectionSession.floatingScanButtonService?.setAlpha(arg1)
                }
            }
        })
        mSeekBarFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(arg0: SeekBar) {
                // TODO Auto-generated method stub

            }

            override fun onStartTrackingTouch(arg0: SeekBar) {
                // TODO Auto-generated method stub

            }

            override fun onProgressChanged(arg0: SeekBar, arg1: Int, arg2: Boolean) {
                // 设置字体变化
                mTvFont.textSize = arg1 + TEXT_SIZE.toFloat()
            }
        })

        // 设置样式选择
        mRbtn1.setOnCheckedChangeListener(this)
        mRbtn2.setOnCheckedChangeListener(this)
        mRbtn3.setOnCheckedChangeListener(this)

        radioTriggerMethods.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.radioTriggerByButton) {
                mRadioTriggerByButton.isChecked = true
                mLayTriggerButtonSettings.visibility = View.VISIBLE
            } else {
                mRadioTriggerByGravity.isChecked = true
                mLayTriggerButtonSettings.visibility = View.GONE
            }
            saveAndRestartService()
        }
        setScreen()
    }


    private fun saveAndRestartService() {
        saveSettings()
        mConnectionSession.endSession()
        // 开启服务
        mConnectionSession.startSession()
        if (mConnectionSession.floatingScanButtonService != null) {
            mConnectionSession.floatingScanButtonService?.onTriggerMethodChange()
        }
        val startTime = System.currentTimeMillis()
        AsTask(
                this@UsbHostSettingActivity,
                {
                    val result = CallResult()
                    var serviceConnected = false
                    while(System.currentTimeMillis() - startTime < 5000 && !serviceConnected)
                    {
                        if(mConnectionSession.floatingScanButtonService != null)
                        {
                            serviceConnected = true
                            break
                        }
                        Thread.sleep(50)
                    }
                    if(!serviceConnected) result.isSuccess = false
                    result
                },
                { context: Context, result: CallResult ->
                    if(result.isSuccess) {
                        mConnectionSession.connect()
                        mConnectionSession.setConnectListener(object : CommunicateListener {
                            override fun onDisconnected() {
                                MessageBox.showInfoMessage(this@UsbHostSettingActivity, "Device has disconnected")
                            }
                            override fun onDataReceived(data: String) {
                                mTvData.append(data)
                            }

                            override fun onConnectFailure(errorMessage: String) {
                                MessageBox.showWarningMessage(this@UsbHostSettingActivity, errorMessage)
                            }
                        })
                    }
                    else
                    {
                        MessageBox.showWarningMessage(this@UsbHostSettingActivity, R.string.usb_host_service_not_start)
                    }
                },
                null
        ).go()
    }


    /**
     * 设置屏幕转屏
     */
    private fun setScreen() {
        //this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
    }

    private fun readSettings() {

        if (UsbHostReferences.triggerMethod == UsbHostConsts.USB_HOST_TRIGGER_METHOD_BUTTON) {
            mRadioTriggerByButton.isChecked = true
            mLayTriggerButtonSettings.visibility = View.VISIBLE
        } else {
            mRadioTriggerByGravity.isChecked = true
            mLayTriggerButtonSettings.visibility = View.GONE
        }



        mCkbVibrate.isChecked = UsbHostReferences.isVibrate

        // 设置透明度
        mSeekBar.progress = UsbHostReferences.triggerButtonAlpha
        mSeekBarFont.progress = UsbHostReferences.triggerButtonTextSize
        mTvFont.textSize = UsbHostReferences.triggerButtonTextSize + TEXT_SIZE.toFloat()
        mEdtVibrateTime.setText(UsbHostReferences.vibrateTime.toString())

        // 显示默认样式
        when (UsbHostReferences.triggerButtonBackground) {
            1 -> mRbtn1.isChecked = true
            2 -> mRbtn2.isChecked = true
            3 -> mRbtn3.isChecked = true
        }
    }

    private fun saveSettings() {
        if (mRadioTriggerByButton.isChecked) {
            UsbHostReferences.triggerMethod = UsbHostConsts.USB_HOST_TRIGGER_METHOD_BUTTON
        }

        if (mRadioTriggerByGravity.isChecked) {
            UsbHostReferences.triggerMethod = UsbHostConsts.USB_HOST_TRIGGER_METHOD_GRAVITY
        }

        UsbHostReferences.isVibrate = mCkbVibrate.isChecked

        UsbHostReferences.triggerButtonAlpha = mSeekBar.progress
        UsbHostReferences.triggerButtonTextSize = mSeekBarFont.progress
        UsbHostReferences.vibrateTime = mEdtVibrateTime.text.toString().ifNullOrBlank("50").toLong()

    }
}
