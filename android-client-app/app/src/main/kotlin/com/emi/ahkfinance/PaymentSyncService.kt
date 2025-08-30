package com.emi.ahkfinance

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

class PaymentSyncService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PaymentSyncService", "Payment sync service started")
        
        // Schedule periodic sync work
        schedulePeriodicSync()
        
        return START_STICKY
    }
    
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = PeriodicWorkRequestBuilder<PaymentSyncWorker>(
            15, TimeUnit.MINUTES // Check every 15 minutes
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
            
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "payment_sync_work",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }
    
    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, PaymentSyncService::class.java)
            context.startService(intent)
        }
    }
}

class PaymentSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        return try {
            Log.d("PaymentSyncWorker", "Starting payment link sync")
            
            // Get device ID from shared preferences
            val deviceId = PaymentManager.getDeviceId(applicationContext)
            if (deviceId.isNullOrEmpty()) {
                Log.w("PaymentSyncWorker", "Device ID not found, skipping sync")
                return Result.success()
            }
            
            // Sync payment link from Firebase
            PaymentManager.syncPaymentLinkFromFirebase(applicationContext, deviceId) { paymentLink ->
                if (paymentLink != null) {
                    Log.d("PaymentSyncWorker", "Payment link sync completed successfully: $paymentLink")
                } else {
                    Log.e("PaymentSyncWorker", "Payment link sync failed")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("PaymentSyncWorker", "Error during payment sync", e)
            Result.retry()
        }
    }
}