package com.custom.pioneerlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var webView: WebView
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 101

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
        }

        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("file:///android_asset/index.html")

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine) {
            startSpeedUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                locationPermissionCode
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSpeedUpdates() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    this
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startSpeedUpdates()
        }
    }

    override fun onLocationChanged(location: Location) {
        val speedKmh = if (location.hasSpeed()) {
            val rawKmh = location.speed * 3.6f
            if (rawKmh < 1.5f) 0 else rawKmh.roundToInt()
        } else {
            0
        }

        runOnUiThread {
            webView.evaluateJavascript("if (typeof updateSpeed === 'function') updateSpeed($speedKmh);", null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Залишаємо користувача в лаучері
    }

    inner class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun launchApp(packageName: String) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "Додаток не знайдено: $packageName", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun getInstalledApps(): String {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val appsList = pm.queryIntentActivities(intent, 0)
            val jsonArray = JSONArray()

            for (resolveInfo in appsList) {
                val appObj = JSONObject()
                appObj.put("name", resolveInfo.loadLabel(pm).toString())
                appObj.put("package", resolveInfo.activityInfo.packageName)
                jsonArray.put(appObj)
            }
            return jsonArray.toString()
        }

        @JavascriptInterface
        fun sendMediaKey(action: String) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val keyCode = when (action) {
                "play" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
                "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                else -> return
            }
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }

        @JavascriptInterface
        fun controlRadio(action: String, frequency: Double) {
            try {
                val intent = Intent("com.ts.main.radio").apply {
                    putExtra("action", action)
                    putExtra("frequency", frequency)
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
