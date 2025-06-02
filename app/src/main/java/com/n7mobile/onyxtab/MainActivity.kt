package com.n7mobile.onyxtab

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.n7mobile.onyxtab.databinding.ActivityMainBinding
import com.n7mobile.onyxtab.helpers.MemoryMonitor
import com.n7mobile.onyxtab.helpers.TimeHelper
import com.n7mobile.onyxtab.watchdog.WatchdogService
import com.onyx.android.sdk.api.device.EpdDeviceManager
import com.onyx.android.sdk.api.device.brightness.BaseBrightnessProvider
import com.onyx.android.sdk.api.device.brightness.BrightnessController
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import com.onyx.android.sdk.device.BaseDevice
import com.onyx.android.sdk.utils.DeviceUtils
import org.threeten.bp.Duration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val TAG = "n7.MainActivity"
        private const val HA_URL = "http://192.168.1.200:8123/dashboard-tablet/0?kiosk"
    }

    private lateinit var binding: ActivityMainBinding

    private val interval = 1 * 60 * 1000L   // 1 min
    private val runnable: Runnable = Runnable {
        refreshScreen()
        adjustBrightness()
    }
    private val handler = Handler(Looper.getMainLooper())
    private val memoryMonitor = MemoryMonitor(Duration.ofSeconds(300L), 3)
    private var brightnessProvider: BaseBrightnessProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EpdController.enablePost(binding.root, 1)

        // set full update after how many partial update
        EpdDeviceManager.setGcInterval(5)

        DeviceUtils.setFullScreenOnResume(this, true)

        initWebView()
        initButtons()
        initLightSensor()
        initBrightnessControl()

        printExitReason()

        memoryMonitor.start()

        val serviceIntent = Intent(this, WatchdogService::class.java)
        startForegroundService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        handler.post(runnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    private fun printExitReason(){
        Log.d(TAG, "Trying to get last exit reason")
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val applicationExitInfos = am.getHistoricalProcessExitReasons(null, 0, 0)

        for (exitInfo in applicationExitInfos) {
            Log.e(TAG, String.format("%s: Exit reason: %d, description: %s", formatTimestampToReadableDate(exitInfo.timestamp), exitInfo.reason, exitInfo.description))
        }
    }

    private fun formatTimestampToReadableDate(timestamp: Long): String {
        val date = Date(timestamp)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun refreshScreen(){
        Log.d(TAG, "Refreshing screen")
        EpdController.repaintEveryThing(UpdateMode.GC)
        handler.postDelayed(runnable, interval)
    }

    private fun adjustBrightness(){
        if (TimeHelper.isNightTime()){
            checkAndSetBrigthness(1)
        }
        else if (TimeHelper.isEveningTime() || TimeHelper.isMorningTime()){
            checkAndSetBrigthness(5)
        } else {
            checkAndSetBrigthness(10)
        }
    }

    private fun checkAndSetBrigthness(value: Int){
        if (brightnessProvider?.value != value) {
            Log.d(TAG, "Setting brightness to $value.")
            brightnessProvider?.value = value
        }
    }

    private fun initBrightnessControl() {
        val brightnessType = BrightnessController.getBrightnessType(this)
        Log.d(TAG, "Brightness type: $brightnessType")
        brightnessProvider = BrightnessController.getBrightnessProvider(this, BaseDevice.LIGHT_TYPE_CTM_BRIGHTNESS)
        Log.d(TAG, "Brigthness: ${brightnessProvider?.value}, index: ${brightnessProvider?.index}, maxIndex: ${brightnessProvider?.maxIndex}")
    }

    private fun initLightSensor(){
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor != null){
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.w(TAG, "Light sensor not available")
        }
    }

    private fun initButtons(){
        binding.btnNormal.setOnClickListener {
            EpdController.clearAppScopeUpdate()
            EpdController.applyAppScopeUpdate(TAG, false, true, UpdateMode.None, Int.MAX_VALUE)
        }

        binding.btnRepaint.setOnClickListener {
            EpdController.repaintEveryThing(UpdateMode.GC)
        }

        binding.btnEnterFast.setOnClickListener {
            EpdDeviceManager.enterAnimationUpdate(true)
        }

        binding.btnExitFast.setOnClickListener {
            EpdDeviceManager.exitAnimationUpdate(true)
        }
    }


    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(){
        Log.d(TAG, "Physical dimensions: ${display.width} x ${display.height}. Density: ${resources.displayMetrics.density}, densityDpi: ${resources.displayMetrics.densityDpi}")
        Log.d(TAG, "Scaled dimensions: ${display.width/resources.displayMetrics.density} x ${display.height/resources.displayMetrics.density}")

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webview, true)
        }

        binding.webview.apply {
            settings.apply {
                allowContentAccess = true
                javaScriptEnabled = true
                domStorageEnabled = true
                setRenderPriority(WebSettings.RenderPriority.HIGH)
            }

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    Log.w(TAG, "onReceivedError. Requesct: ${request?.url}, error: ${error?.description}")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "onPageFinished: $url")

                    // normal mode
                    EpdController.clearAppScopeUpdate()
                    EpdController.applyAppScopeUpdate(TAG, false, true, UpdateMode.None, Int.MAX_VALUE)
                }

                override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm)
                    Log.d(TAG, "onReceivedHttpAuthRequest")
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    Log.w(TAG, "onReceivedHttpError, request: $request, error: $errorResponse")
                }

                override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?, args: String?) {
                    super.onReceivedLoginRequest(view, realm, account, args)
                    Log.d(TAG, "onReceivedLoginRequest")
                }

            }

            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            loadUrl(HA_URL)
        }
    }

    override fun onSensorChanged(se: SensorEvent?) {
        if (se?.sensor?.type == Sensor.TYPE_LIGHT){
            Log.v(TAG, "Light: ${se.values[0]}")
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // ignored
    }
}