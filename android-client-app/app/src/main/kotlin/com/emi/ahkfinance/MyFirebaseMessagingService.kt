package com.emi.ahkfinance

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val command = remoteMessage.data["command"]
        if (command == "lock" || command == "unlock") {
            val prefs = getSharedPreferences("ahk_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isLocked", command == "lock").apply()
            // Optionally show notification
            showNotification(command)
        }
    }

    private fun showNotification(command: String?) {
        val channelId = "fcm_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "FCM", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AHK Finance")
            .setContentText(if (command == "lock") "ডিভাইস লক করা হয়েছে" else "ডিভাইস আনলক করা হয়েছে")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        manager.notify(2, notification)
    }
}
