package com.ambientsync.companion

import android.app.*
import android.content.Intent
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import kotlin.math.sqrt

class CaptureService : Service() {
    companion object {
        const val ACTION_LEVEL = "com.ambientsync.companion.LEVEL"
        private const val CHANNEL = "capture"
    }
    private var projection: MediaProjection? = null
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_NOT_STICKY
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Audio capture test", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Ambient Sync testing audio")
            .setContentText("Tap Stop in the app to end capture")
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(1, notification)
        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra("resultData", Intent::class.java)
        else intent?.getParcelableExtra<Intent>("resultData")
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        try {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = projectionManager.getMediaProjection(code, data)
            val config = AudioPlaybackCaptureConfiguration.Builder(projection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build()
            val sampleRate = 16000
            val minBytes = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            require(minBytes > 0)
            recorder = AudioRecord.Builder()
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                .setBufferSizeInBytes(maxOf(minBytes, 4096))
                .setAudioPlaybackCaptureConfig(config)
                .build()
            require(recorder?.state == AudioRecord.STATE_INITIALIZED)
            running = true
            worker = Thread {
                val buffer = ShortArray(2048)
                try {
                    recorder?.startRecording()
                    while (running) {
                        val n = recorder?.read(buffer, 0, buffer.size) ?: -1
                        if (n <= 0) break
                        var sum = 0.0
                        for (i in 0 until n) { val v = buffer[i] / 32768.0; sum += v * v }
                        val rms = sqrt(sum / n)
                        val level = (rms * 300).toInt().coerceIn(0, 100)
                        sendBroadcast(Intent(ACTION_LEVEL).setPackage(packageName).putExtra("level", level))
                    }
                } catch (_: Exception) { /* Stop gracefully if capture becomes unavailable. */ }
                stopSelf()
            }.apply { start() }
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        running = false
        recorder?.run { try { stop() } catch (_: Exception) {}; release() }
        recorder = null
        projection?.stop()
        projection = null
        super.onDestroy()
    }
}
