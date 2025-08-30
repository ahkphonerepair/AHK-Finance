package com.emi.ahkfinance

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseModule {
    private const val DEFAULT_PAYMENT_LINK = "https://shop.bkash.com/ahk-phone-repair01630138471/paymentlink"
    
    fun init(context: Context) {
        FirebaseApp.initializeApp(context)
        FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
    }

    fun updateDeviceStatus(deviceId: String, status: Map<String, Any>) {
        val db = FirebaseFirestore.getInstance()
        val statusWithPaymentLink = status.toMutableMap()
        
        // Add default payment link if not present
        if (!statusWithPaymentLink.containsKey("paymentLink")) {
            statusWithPaymentLink["paymentLink"] = DEFAULT_PAYMENT_LINK
        }
        
        db.collection("devices").document(deviceId).update(statusWithPaymentLink)
            .addOnSuccessListener { 
                Log.d("Firebase", "Status updated successfully: $statusWithPaymentLink") 
            }
            .addOnFailureListener { e -> 
                Log.e("Firebase", "Error updating status: $statusWithPaymentLink", e)
                // Retry with set() if document doesn't exist
                db.collection("devices").document(deviceId).set(statusWithPaymentLink)
                    .addOnSuccessListener { Log.d("Firebase", "Status set successfully after update failed") }
                    .addOnFailureListener { retryError -> Log.e("Firebase", "Error setting status after retry", retryError) }
            }
    }

    private var lastKnownLockState: Boolean? = null
    
    fun listenForLockUnlock(deviceId: String, onLock: () -> Unit, onUnlock: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("devices").document(deviceId)
            .addSnapshotListener { snapshot, _ ->
                val lockState = snapshot?.getBoolean("locked") ?: false
                
                // Only trigger callbacks if the state has actually changed
                if (lastKnownLockState != lockState) {
                    Log.d("Firebase", "Lock state changed from $lastKnownLockState to $lockState")
                    lastKnownLockState = lockState
                    
                    if (lockState) {
                        onLock()
                    } else {
                        onUnlock()
                    }
                } else {
                    Log.d("Firebase", "Lock state unchanged: $lockState, ignoring")
                }
            }
    }
    
    fun updateLastOnlineTime(deviceId: String) {
        val db = FirebaseFirestore.getInstance()
        val currentTimestamp = System.currentTimeMillis()
        
        db.collection("devices").document(deviceId).update(
            mapOf("lastSeenTimestamp" to currentTimestamp)
        )
        .addOnSuccessListener {
            Log.d("Firebase", "Last online time updated successfully: $currentTimestamp")
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error updating last online time", e)
            // Retry with set() if document doesn't exist
            db.collection("devices").document(deviceId).set(
                mapOf("lastSeenTimestamp" to currentTimestamp)
            )
            .addOnSuccessListener { Log.d("Firebase", "Last online time set successfully after update failed") }
            .addOnFailureListener { retryError -> Log.e("Firebase", "Error setting last online time after retry", retryError) }
        }
    }
    
    fun uploadOfflineUnlockCount(context: Context, deviceId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                "ahk_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            val offlineUnlockCount = prefs.getInt("offlineUnlockCount", 0)
            
            if (offlineUnlockCount > 0) {
                val db = FirebaseFirestore.getInstance()
                db.collection("devices").document(deviceId).update(
                    mapOf(
                        "offlineUnlockCount" to offlineUnlockCount,
                        "lastOfflineUnlockSync" to System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {
                    Log.d("Firebase", "Offline unlock count uploaded successfully: $offlineUnlockCount")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error uploading offline unlock count", e)
                    onFailure(e)
                }
            } else {
                Log.d("Firebase", "No offline unlocks to upload")
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error reading offline unlock count", e)
            onFailure(e)
        }
    }
    
    fun getPaymentLink(deviceId: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit = {}) {
        val db = FirebaseFirestore.getInstance()
        db.collection("devices").document(deviceId).get()
            .addOnSuccessListener { document ->
                val paymentLink = document.getString("paymentLink") ?: DEFAULT_PAYMENT_LINK
                Log.d("Firebase", "Payment link retrieved: $paymentLink")
                onSuccess(paymentLink)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error getting payment link", e)
                onFailure(e)
            }
    }
    
    fun updatePaymentLink(deviceId: String, paymentLink: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val db = FirebaseFirestore.getInstance()
        db.collection("devices").document(deviceId).update(
            mapOf(
                "paymentLink" to paymentLink,
                "paymentLinkUpdated" to System.currentTimeMillis()
            )
        )
        .addOnSuccessListener {
            Log.d("Firebase", "Payment link updated successfully: $paymentLink")
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error updating payment link", e)
            onFailure(e)
        }
    }
    
    fun listenForPaymentLinkChanges(deviceId: String, onPaymentLinkChanged: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("devices").document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firebase", "Error listening for payment link changes", error)
                    return@addSnapshotListener
                }
                
                val paymentLink = snapshot?.getString("paymentLink") ?: DEFAULT_PAYMENT_LINK
                Log.d("Firebase", "Payment link changed: $paymentLink")
                onPaymentLinkChanged(paymentLink)
            }
    }
    
    fun getDefaultPaymentLink(): String {
        return DEFAULT_PAYMENT_LINK
    }

    // Admin API endpoint to update payment link for a specific device
    fun updatePaymentLinkForDevice(deviceId: String, newPaymentLink: String, callback: (Boolean) -> Unit) {
        if (deviceId.isEmpty() || newPaymentLink.isEmpty()) {
            callback(false)
            return
        }

        val deviceRef = FirebaseFirestore.getInstance().collection("devices").document(deviceId)
        val updates = hashMapOf<String, Any>(
            "paymentLink" to newPaymentLink,
            "paymentLinkUpdatedAt" to FieldValue.serverTimestamp()
        )

        deviceRef.update(updates)
            .addOnSuccessListener {
                Log.d("FirebaseModule", "Payment link updated successfully for device: $deviceId")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModule", "Failed to update payment link for device: $deviceId", exception)
                callback(false)
            }
    }

    // Admin API endpoint to update payment link for all devices
    fun updatePaymentLinkForAllDevices(newPaymentLink: String, callback: (Boolean, Int) -> Unit) {
        if (newPaymentLink.isEmpty()) {
            callback(false, 0)
            return
        }

        FirebaseFirestore.getInstance().collection("devices")
            .get()
            .addOnSuccessListener { documents ->
                val batch = FirebaseFirestore.getInstance().batch()
                var updateCount = 0

                for (document in documents) {
                    val deviceRef = FirebaseFirestore.getInstance().collection("devices").document(document.id)
                    batch.update(deviceRef, mapOf(
                        "paymentLink" to newPaymentLink,
                        "paymentLinkUpdatedAt" to FieldValue.serverTimestamp()
                    ))
                    updateCount++
                }

                if (updateCount > 0) {
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("FirebaseModule", "Payment link updated for $updateCount devices")
                            callback(true, updateCount)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirebaseModule", "Failed to update payment links", exception)
                            callback(false, 0)
                        }
                } else {
                    callback(true, 0)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModule", "Failed to fetch devices for payment link update", exception)
                callback(false, 0)
            }
    }

    // Get all devices with their payment links (for admin dashboard)
    fun getAllDevicesWithPaymentLinks(callback: (List<Map<String, Any>>) -> Unit) {
        FirebaseFirestore.getInstance().collection("devices")
            .get()
            .addOnSuccessListener { documents ->
                val devicesList = mutableListOf<Map<String, Any>>()
                for (document in documents) {
                    val deviceData = document.data.toMutableMap()
                    deviceData["deviceId"] = document.id
                    devicesList.add(deviceData)
                }
                callback(devicesList)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModule", "Failed to fetch devices", exception)
                callback(emptyList())
            }
    }
}