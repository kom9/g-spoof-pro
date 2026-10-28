package com.gspoof.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * Foreground service that keeps the spoofing loop alive while the user
 * is in Google Maps / Waze or the screen is off.
 */
class SpoofService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL, "Spoofing", NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        when (intent?.action) {
            ACTION_STOP -> {
                SpoofBridge.stop()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val ok = SpoofBridge.start(
                    this,
                    intent.getDoubleExtra(EXTRA_LAT, 0.0),
                    intent.getDoubleExtra(EXTRA_LON, 0.0),
                    intent.getFloatExtra(EXTRA_SPEED, 0f),
                    intent.getFloatExtra(EXTRA_BEARING, 0f),
                    intent.getBooleanExtra(EXTRA_SIMULATE, true)
                )
                if (!ok) {
                    stopForegroundCompat()
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, SpoofService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("G-Spoof Pro aktif")
            .setContentText("Lokasi palsu sedang dikirim ke semua aplikasi")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    null, "STOP", stop
                ).build()
            )
            .build()
    }

    override fun onDestroy() {
        SpoofBridge.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.gspoof.pro.action.START"
        const val ACTION_STOP = "com.gspoof.pro.action.STOP"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_BEARING = "bearing"
        const val EXTRA_SIMULATE = "simulate"
        private const val CHANNEL = "gspoof_spoofing"
        private const val NOTIF_ID = 1001
    }
}
