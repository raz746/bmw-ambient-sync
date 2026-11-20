package com.ambientsync.companion

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import kotlin.math.*

class MicService : Service() {
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_NOT_STICKY
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel("mic", "Ambient Sync microphone", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, "mic")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Ambient Sync microphone active")
            .setContentText("Open Ambient Sync and tap Stop to end").build()
        startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        try {
            val rate = 16000
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            require(min > 0)
            recorder = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min, 8192))
            require(recorder?.state == AudioRecord.STATE_INITIALIZED)
            running = true
            worker = Thread {
                val buffer = ShortArray(2048)
                var smoothed = 0.0
                var baseline = 0.0
                var peak = 0.0001
                var pulse = 0.0
                var lastBeatMs = 0L
                try {
                    recorder?.startRecording()
                    while (running) {
                        val count = recorder?.read(buffer, 0, buffer.size) ?: -1
                        if (count <= 0) break
                        // Goertzel filter bank: approximate energy in 60–180 Hz.
                        // This is bass intensity, not BPM/beat timing.
                        val frequencies = doubleArrayOf(65.0, 90.0, 120.0, 155.0, 180.0)
                        var energy = 0.0
                        for (hz in frequencies) {
                            val omega = 2.0 * PI * hz / rate
                            val coeff = 2.0 * cos(omega)
                            var s1 = 0.0; var s2 = 0.0
                            for (i in 0 until count) {
                                val sample = (buffer[i] / 32768.0) * (0.5 - 0.5 * cos(2.0 * PI * i / (count - 1)))
                                val s = sample + coeff * s1 - s2
                                s2 = s1; s1 = s
                            }
                            energy += max(0.0, s1 * s1 + s2 * s2 - coeff * s1 * s2)
                        }
                        val amplitude = sqrt(energy / frequencies.size) / count
                        baseline = if (baseline == 0.0) amplitude else baseline * 0.985 + amplitude * 0.015
                        peak = max(amplitude, peak * 0.995)
                        val relative = ((amplitude - baseline * 0.7) / max(peak, 0.00001)).coerceIn(0.0, 1.0)
                        smoothed = 0.70 * smoothed + 0.30 * relative
                        val level = (smoothed * 100).roundToInt().coerceIn(0, 100)
                        val now = SystemClock.elapsedRealtime()
                        val onset = amplitude > baseline * 1.65 && amplitude > 0.00001
                        if (onset && now - lastBeatMs > 220L) {
                            pulse = 1.0
                            lastBeatMs = now
                        } else {
                            pulse *= 0.65
                        }
                        if (amplitude < 0.00001) { pulse = 0.0; smoothed *= 0.7 }
                        sendBroadcast(Intent(CaptureService.ACTION_LEVEL).setPackage(packageName)
                            .putExtra("level", level).putExtra("pulse", (pulse * 100).roundToInt()).putExtra("source", "Mic bass"))
                    }
                } catch (_: Exception) { }
                stopSelf()
            }.apply { start() }
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        running = false
        recorder?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        recorder = null
        super.onDestroy()
    }
}
