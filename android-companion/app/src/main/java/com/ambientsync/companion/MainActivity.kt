package com.ambientsync.companion

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.*

class MainActivity : Activity() {
    private val captureRequest = 42
    private val micRequest = 43
    private lateinit var meter: ProgressBar
    private lateinit var status: TextView
    private lateinit var light: TextView
    private val updates = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val value = intent?.getIntExtra("level", 0) ?: 0
            val source = intent?.getStringExtra("source") ?: "Playback"
            meter.progress = value
            status.text = "$source intensity: $value%"
            light.alpha = 0.15f + value / 118f
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 24)
            setBackgroundColor(Color.rgb(12, 14, 24))
        }
        fun label(value: String, size: Float): TextView = TextView(this).apply {
            text = value; textSize = size; setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 10)
        }
        layout.addView(label("Ambient Sync v0.2", 28f))
        layout.addView(label("Mic bass detector + playback capture test. Audio is processed locally and never stored.", 15f))
        light = label("━━━━━━━━━━━━", 35f).apply { setTextColor(Color.rgb(158, 109, 255)); alpha = 0.15f }
        layout.addView(light)
        meter = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        layout.addView(meter, LinearLayout.LayoutParams(-1, 35))
        status = label("Idle", 16f)
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "Start microphone bass detection"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureService::class.java))
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startMic()
                else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
            }
        })
        layout.addView(Button(this).apply {
            text = "Test internal playback capture"
            setOnClickListener {
                stopService(Intent(this@MainActivity, MicService::class.java))
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mgr.createScreenCaptureIntent(), captureRequest)
            }
        })
        layout.addView(Button(this).apply {
            text = "Stop"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureService::class.java))
                stopService(Intent(this@MainActivity, MicService::class.java))
                meter.progress = 0; light.alpha = 0.15f; status.text = "Stopped"
            }
        })
        layout.addView(label("Test with the car parked. Microphone mode may detect speech or road noise; it is not connected to BMW lighting.", 13f))
        setContentView(layout)
    }
    private fun startMic() {
        startForegroundService(Intent(this, MicService::class.java))
        status.text = "Starting microphone detector…"
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == micRequest) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startMic()
            else status.text = "Microphone permission denied"
        }
    }
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(CaptureService.ACTION_LEVEL)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(updates, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(updates, filter)
    }
    override fun onStop() { unregisterReceiver(updates); super.onStop() }
    @Deprecated("Android permission result callback for minimum SDK 29")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != captureRequest) return
        if (resultCode != RESULT_OK || data == null) { status.text = "Permission not granted"; return }
        startForegroundService(Intent(this, CaptureService::class.java).apply {
            putExtra("resultCode", resultCode); putExtra("resultData", data)
        })
        status.text = "Playback capture requested"
    }
}
