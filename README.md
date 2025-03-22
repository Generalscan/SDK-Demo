# SDK-Demo

This is a demo project for Generalscan scanner SDK


## Download SDK aar

Download from here : [SDK aar](https://raw.githubusercontent.com/Generalscan/SDK-Demo/main/download/generalscan-sdk.zip)

## Import SDK into android studio project
1) Copy the SDK aar file to app/libs
2) Add the flatDir setting Gradle configuration to your Android project. In your root `build.gradle` file:
```groovy
allprojects { 
	repositories 
		{ 
			jcenter() 
			flatDir { dirs 'libs' }  // add flatDir setting
		} 
}
```


3) Open app level build.gradle file and add .aar file
```groovy
dependencies 
{
    implementation files('libs/scannersdk-release.aar')
}
```


4) Add the follow permissions into manifest.xml
```xml
 <!-- Bluetooth Permission -->
<!-- Request legacy Bluetooth permissions on older devices.  maxSdkVersion="30" Android 11 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <!-- Needed only if your app makes the device discoverable to Bluetooth devices. -->
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

    <!-- Needed only if your app looks for Bluetooth devices. -->
    <!-- Include android:usesPermissionFlags="neverForLocation"  only if you can strongly assert that
     your app never derives physical location from Bluetooth scan results. -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>

    <!-- Needed only if your app communicates with already-paired Bluetooth devices. -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<!-- 服务中弹出对话框 -->
<!-- Show Alert Dialog in Service -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```


5) Add the follow service declarations into manifest.xml
```xml
 <service
    android:name="com.generalscan.scannersdk.core.session.bluetooth.service.BluetoothConnectService"
    android:enabled="true"
    android:exported="true" >
</service>
```

6)Add the following line when app is started
```kotlin
SdkContext.initSdk(this, null)
```
# Work with bluetooth scanner
1) Turn on Bluetooth
2) Request for bluetooth permissions in the code 
```kotlin
var permissions: MutableList<String> = ArrayList()
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
}
/*
if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
    permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
}
*/
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
    permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
    permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    permissions.add(Manifest.permission.BLUETOOTH_SCAN)
    //permissions.add(Manifest.permission.CAMERA)
}
//TODO: Request for above permission
```
3) Request overlay permission
```kotlin
 val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()))
 startActivityForResult(intent, requestCode)
```
4) Start Bluetooth session after activity created
```kotlin
val bluetoothConnectSession = BluetoothConnectSession(this)
//Setup session listener	
//设置 Session 监听
bluetoothConnectSession.sessionListener =object : SessionListener{

    //When session is ready
    override fun onSessionReady(session: IConnectSession) {
        
        //设置蓝牙监听器
        //Setup listener to receive data
        bluetoothConnectSession.setConnectListener(
            object : CommunicateListener {

                //蓝牙设备连接成功
                //Bluetooth device successfully connected.
                override fun onConnected() {
                    //TODO: On device connected
                }

                //蓝牙设备断开
                //Bluetooth device disconnected
                override fun onDisconnected() {
                    //TODO on device disconnected
                }
                
                //蓝牙设备连接失败
                //Bluetooth device connection failed.
                override fun onConnectFailure(errorMessage: String) {
                    //TODO: On device connect failure

                }


                //接收到扫描器数据
                //Scanner data received
                override fun onDataReceived(data: String) {

                }
                
                //原始数据接收
                //Raw data receive
                override fun onRawDataReceived(data: Byte) {
                    //TODO: On data received from device
                }

                //电池数据接收
                //Battery data receive
                override fun onBatteryDataReceived(voltage: String, percentage: String) {
                    //mTxtReceiveData.append(voltage + ":" + percentage);
                }
               
                
                //蓝牙命令返回数据
                //Bluetooth command callback
                override fun onCommandCallback(name: String, data: String) {
                  
                }
                //扫描器命令超时
                //Scanner command timeout
                override fun onCommandNoResponse(errorMessage: String) {

                }
            }
        )
    }
    
    //When session service initialization timeout
    override fun onSessionStartTimeOut(session: IConnectSession) {
        MessageBox.showWarningMessage(this@BluetoothMainActivity, "Session Timeout")
    }
};

bluetoothConnectSession.startSession()
```

5) Pair Bluetooth device

6) Connect Bluetooth device
```kotlin
val device = bluetoothAdapter!!.getRemoteDevice(selectedDeviceAddress)
bluetoothConnectSession.bluetoothDeviceToConnect = device
bluetoothConnectSession.deviceType = "BLE" //or "SPP"
bluetoothConnectSession.connect()
```

7) Stop Bluetooth session after activity is destroy
```kotlin
//Send current bluetooth session
bluetoothConnectSession.endSession()
```