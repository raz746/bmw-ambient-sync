package com.ambientsync.companion

import android.app.Activity
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import android.graphics.Color

class MainActivity : Activity() {
    private val requestCode = 42
    private lateinit var meter: ProgressBar
    private lateinit var status: TextView
    private val updates = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val value = intent?.getIntExtra("level", 0) ?: 0
            meter.progress = value
            status.text = "Captured audio level: $value% (0% may mean Pandora blocks capture)"
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 60, 36, 24)
            setBackgroundColor(Color.rgb(12, 14, 24))
        }
        fun label(text: String, size: Float): TextView = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(Color.WHITE)
            setPadding(0, 12, 0, 12)
        }
        layout.addView(label("Ambient Sync", 28f))
        layout.addView(label("Pandora playback-capture feasibility test. No audio is saved or uploaded.", 15f))
        meter = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        layout.addView(meter, LinearLayout.LayoutParams(-1, 40))
        status = label("Idle", 15f)
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "Test playback capture"
            setOnClickListener {
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mgr.createScreenCaptureIntent(), requestCode)
            }
        })
        layout.addView(Button(this).apply {
            text = "Stop test"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureService::class.java))
                meter.progress = 0
                status.text = "Stopped"
            }
        })
        layout.addView(label("Start Pandora playback, then grant the Android capture permission. A zero meter is inconclusive: the source may block capture or Android Auto may use a different audio route.", 13f))
        setContentView(layout)
    }
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(CaptureService.ACTION_LEVEL)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(updates, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(updates, filter)
    }
    override fun onStop() {
        unregisterReceiver(updates)
        super.onStop()
    }
    @Deprecated("Android permission result callback for minimum SDK 29")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != this.requestCode) return
        if (resultCode != RESULT_OK || data == null) {
            status.text = "Permission not granted"
            return
        }
        val service = Intent(this, CaptureService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("resultData", data)
        }
        startForegroundService(service)
        status.text = "Capture requested. Watch for meter activity."
    }
}
