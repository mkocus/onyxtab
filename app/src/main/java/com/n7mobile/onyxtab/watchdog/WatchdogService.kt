package com.n7mobile.onyxtab.watchdog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.n7mobile.onyxtab.MainActivity
import com.n7mobile.onyxtab.R

class WatchdogService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 5_000L

    companion object {
        private const val TAG = "n7.WatchdogService"
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        handler.post(checkActivityRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkActivityRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundService() {
        Log.d(TAG, "Starting watchdog service")
        val notificationChannelId = "WATCHDOG_SERVICE_CHANNEL"
        val channel = NotificationChannel(
                notificationChannelId,
                "Watchdog Service",
                NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Watchdog Service")
            .setContentText("Monitoring activity state...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        try {
            startForeground(1, notification)
        } catch (e: Exception){
            Log.w(TAG, "Cannot start service because of exception", e)
        }
    }

    private val checkActivityRunnable = object : Runnable {
        override fun run() {
            if (!isActivityRunning(MainActivity::class.java)) {
                Log.w(TAG, "Detected that main activity is not running. Restarting!")
                restartActivity(MainActivity::class.java)
            }
            handler.postDelayed(this, checkInterval)
        }
    }

    private fun isActivityRunning(activityClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val tasks = activityManager.getRunningTasks(Integer.MAX_VALUE)
        for (task in tasks) {
            if (task.topActivity?.className == activityClass.name) {
                return true
            }
        }
        return false
    }

    private fun restartActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}