package com.generalscan.sdkapp.system.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import cn.jimex.fms.support.utils.SdkLogUtils


import com.generalscan.scannersdk.R
import com.generalscan.scannersdk.core.basic.interfaces.IConnectSession
import com.generalscan.scannersdk.core.pref.UsbHostReferences
import com.generalscan.scannersdk.core.session.usbhost.basic.UsbHostConsts
import com.generalscan.scannersdk.core.session.usbhost.service.SensorManagerHelper
import com.generalscan.scannersdk.support.utils.SDKUIUtils
import com.generalscan.scannersdk.support.utils.TipHelper
import com.generalscan.sdkapp.support.utils.AppLogUtils


class FloatingScanButtonService : Service(), OnTouchListener {

    /** 基本接口 */
    protected var mConnectSession: IConnectSession? = null

    private var mFloatWindow: ViewGroup? = null
    private var mIsInitialed = false

    private var wmParams: LayoutParams? = null
    // 创建浮动窗口设置布局参数的对象
    private var mWindowManager: WindowManager? = null

    private var mFloatButton: Button? = null

    private var mStartX: Float = 0.toFloat()
    private var mStartY: Float = 0.toFloat()
    private var mOriginX: Int = 0
    private var mOriginY: Int = 0
    private var isAddedToWindow: Boolean = false

    private lateinit var mContext: Context

    internal var sensorHelper: SensorManagerHelper? = null

    internal var isMove = false

    private var isStop = false

    fun setConnectSession(connectSession: IConnectSession)
    {
        mConnectSession = connectSession
    }

    override fun onBind(intent: Intent): IBinder? {
        onRebind(intent)
        return null
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean = true  // we want to use rebind

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        try {
            setForegroundService()
            showFloatWindow()
            AppLogUtils.logInfo("ScheduleService started")
        } catch (e: Throwable) {
            e.printStackTrace()
            AppLogUtils.logError("SchedulerService-onStartCommand", e)
        }
        //resetTheTime()
        return result
    }

    override fun onCreate() {
        super.onCreate()
        isAddedToWindow = false
        mContext = this

        if (!mIsInitialed) {
            if (UsbHostReferences.triggerMethod == UsbHostConsts.USB_HOST_TRIGGER_METHOD_BUTTON) {
                createFloatView()
            } else {
                checkAndCreateSensor()
            }
            mIsInitialed = true
        }
        Log.i(TAG, "service start")
        if (UsbHostReferences.triggerMethod == UsbHostConsts.USB_HOST_TRIGGER_METHOD_BUTTON) {
            showFloatWindow()
        }
    }

    private fun checkAndCreateSensor() {
        if (sensorHelper == null) {
            sensorHelper = SensorManagerHelper(mContext)
            sensorHelper!!.setOnScanListener(
                    object: SensorManagerHelper.OnScanListener{
                        override fun onScan() {
                            sendScanRequest()
                        }
                    })
        }
    }

    private fun enableSensor() {
        checkAndCreateSensor()
        sensorHelper!!.start()
    }

    private fun disableSensor() {
        if (sensorHelper != null)
            sensorHelper!!.stop()
    }


    private fun createFloatView() {
        if (mFloatWindow == null) {
            mFloatWindow = LayoutInflater.from(this).inflate(
                    R.layout.lay_float_button, null) as ViewGroup
            mFloatButton = mFloatWindow!!.findViewById<Button>(R.id.button_floatButton)!!
        }
        wmParams = LayoutParams()
        // 获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = application.getSystemService(
            WINDOW_SERVICE
        ) as WindowManager
        Log.i(TAG, "mWindowManager--->" + mWindowManager!!)
        // 设置window type
        wmParams!!.type = SDKUIUtils.getOverlayType()//alex20171205.n
        //alex20171205.so
        // 设置图片格式，效果为背景透明
        wmParams!!.format = PixelFormat.RGBA_8888
        wmParams!!.flags = LayoutParams.FLAG_NOT_FOCUSABLE
        // 调整悬浮窗显示的停靠位置为中间
        wmParams!!.gravity = Gravity.TOP or Gravity.LEFT
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams!!.x = 0
        wmParams!!.y = 0

        // 设置悬浮窗口长宽数据
        wmParams!!.width = LayoutParams.WRAP_CONTENT
        wmParams!!.height = LayoutParams.WRAP_CONTENT

        mFloatWindow!!.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        Log.i(TAG, "Width/2--->" + mFloatButton!!.measuredWidth / 2)
        Log.i(TAG, "Height/2--->" + mFloatButton!!.measuredHeight / 2)

        // 绑定触摸移动监听
        mFloatButton!!.setOnTouchListener(this)
        mFloatButton!!.setOnClickListener {
            if (!isMove) {
                sendScanRequest()
            }
        }

        setBtnBg(UsbHostReferences.triggerButtonBackground)

    }


    override fun onDestroy() {
        if (UsbHostReferences.triggerMethod == UsbHostConsts.USB_HOST_TRIGGER_METHOD_BUTTON) {
            hideFloatWindow()
        }
        if (sensorHelper != null)
            disableSensor()
        mIsInitialed = false
        isStop = true
        super.onDestroy()
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {

        when (event.action) {

            MotionEvent.ACTION_MOVE -> {
                wmParams!!.x = Math.round(event.rawX - mStartX + mOriginX)
                wmParams!!.y = Math.round(event.rawY - mStartY + mOriginY)
                if (Math.abs(event.rawX - mStartX) > 15 || Math.abs(event.rawY - mStartY) > 15) {
                    isMove = true
                }
            }
            MotionEvent.ACTION_DOWN -> {
                mStartX = event.rawX
                mStartY = event.rawY
                mOriginX = wmParams!!.x
                mOriginY = wmParams!!.y
                isMove = false
            }
            MotionEvent.ACTION_UP -> {
            }
        }
        if (isMove) {
            mWindowManager!!.updateViewLayout(mFloatWindow, wmParams)
        }

        return false
    }

    private fun sendScanRequest() {
        if (UsbHostReferences.isVibrate) {
            TipHelper.Vibrate(mContext, UsbHostReferences.vibrateTime)
        }
        mConnectSession?.sendData("{a}")

    }


    fun onTriggerMethodChange() {
        hideFloatWindow()
        if (UsbHostReferences.triggerMethod == UsbHostConsts.USB_HOST_TRIGGER_METHOD_BUTTON) {
            disableSensor()
            showFloatWindow()
        } else {
            //checkAndCreateSensor();
            enableSensor()
        }
    }

    fun showFloatWindow() {
        disableSensor()
        if (UsbHostReferences.triggerMethod == UsbHostConsts.USB_HOST_TRIGGER_METHOD_BUTTON) {
            if (mIsInitialed && !isAddedToWindow) {
                if (mFloatWindow == null) {
                    createFloatView()
                }
                mWindowManager!!.addView(mFloatWindow, wmParams)
                isAddedToWindow = true
                UsbHostReferences.showTriggerButton = true
                SdkLogUtils.logInfo("FloatingScanButtonService", "Show")
            }
        }
        if (mFloatButton != null) {
            mFloatButton!!.visibility = View.VISIBLE
        }
    }

    fun hideFloatWindow() {
        //if(UsbHostSettings.getUsbHostTriggerMethod(mContext) == UsbHostSettings.USB_HOST_TRIGGER_METHOD_BUTTON) {
        if (mIsInitialed && isAddedToWindow) {
            mWindowManager!!.removeViewImmediate(mFloatWindow)
            isAddedToWindow = false
            UsbHostReferences.showTriggerButton = false
            SdkLogUtils.logInfo("FloatingScanButtonService", "Hide")
        }
        if (mFloatButton != null) {
            mFloatButton!!.visibility = View.GONE
        }
        //}
    }

    fun setAlpha(alpha: Int) {
        mFloatButton!!.background.alpha = alpha
        UsbHostReferences.triggerButtonAlpha = alpha
    }

    @SuppressLint("NewApi")
    fun setBtnBg(index: Int) {
        if (mFloatButton != null) {
            when (index) {
                1 -> mFloatButton!!.setBackgroundResource(R.drawable.btn1)
                2 -> mFloatButton!!.setBackgroundResource(R.drawable.btn2)
                3 ->

                    mFloatButton!!.setBackgroundResource(R.drawable.button_trigger)
            }
            mFloatButton!!.background.alpha = UsbHostReferences.triggerButtonAlpha
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    fun setForegroundService() {
        //设定的通知渠道名称

        //设置通知的重要程度
        val importance = NotificationManager.IMPORTANCE_LOW
        //android.support.v4.app.NotificationCompat.Builder()
        //在创建的通知渠道上发送通知
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, CHANNEL_ID)//CHANNEL_ID
        builder.setContentTitle(getString(R.string.app_name)) //设置通知标题
            .setContentText("${getString(R.string.app_name)} is running") //设置通知内容
            .setAutoCancel(false) //用户触摸时，自动关闭
            .setNumber(0)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setOngoing(true) //设置处于运行状态

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
            channel.description = CHANNEL_NAME
            channel.setShowBadge(false)
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        //将服务置于启动状态 NOTIFICATION_ID指的是创建的通知的ID
        startForeground(992, builder.build())
    }
    companion object {

        private val TAG = FloatingScanButtonService::class.java.simpleName

        private val INTENT_EXTRA_IS_SHOW_WINDOW = "isShowWindow"

        /**
         * Called when the com.generalscan.scanner.activity is first created.
         */
        private val VID = 0x0483
        private val PID = 0x5740// I believe it is 0x0000 for the
        // Arduino Megas


        private const val CHANNEL_NAME = "MobileFFM"
        private const val CHANNEL_ID = "MobileFFM-ForegroundService"

        fun startService(context: Context, customAction: String? = null) {
            try {

                val intent = Intent(context, FloatingScanButtonService::class.java)
                if (!customAction.isNullOrBlank())
                    intent.action = customAction
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                AppLogUtils.logError("startService", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingScanButtonService::class.java)
            context.stopService(intent)
        }
    }
    inner class FloatingScanButtonServiceBinder : Binder() {
        val service: FloatingScanButtonService
            get() = this@FloatingScanButtonService
    }
}
