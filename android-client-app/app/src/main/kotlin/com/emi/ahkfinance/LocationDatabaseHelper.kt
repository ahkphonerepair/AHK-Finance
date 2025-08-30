package com.emi.ahkfinance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class LocationDatabaseHelper private constructor(context: Context) : 
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val TAG = "LocationDatabaseHelper"
        private const val DATABASE_NAME = "location_tracking.db"
        private const val DATABASE_VERSION = 1
        
        // Table name
        private const val TABLE_LOCATIONS = "locations"
        
        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_DEVICE_ID = "device_id"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_ACCURACY = "accuracy"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_SYNCED = "synced"
        
        @Volatile
        private var INSTANCE: LocationDatabaseHelper? = null
        
        fun getInstance(context: Context): LocationDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationDatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_LOCATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DEVICE_ID TEXT NOT NULL,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_ACCURACY REAL NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent()
        
        db.execSQL(createTable)
        Log.d(TAG, "Location tracking database created")
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATIONS")
        onCreate(db)
    }
    
    fun insertLocationData(locationData: LocationData): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DEVICE_ID, locationData.deviceId)
            put(COLUMN_LATITUDE, locationData.latitude)
            put(COLUMN_LONGITUDE, locationData.longitude)
            put(COLUMN_ACCURACY, locationData.accuracy)
            put(COLUMN_TIMESTAMP, locationData.timestamp)
            put(COLUMN_DATE, locationData.date)
            put(COLUMN_TIME, locationData.time)
            put(COLUMN_SYNCED, if (locationData.synced) 1 else 0)
        }
        
        val id = db.insert(TABLE_LOCATIONS, null, values)
        Log.d(TAG, "Location data inserted with ID: $id")
        return id
    }
    
    fun getUnsyncedLocationData(): List<LocationData> {
        val db = readableDatabase
        val locations = mutableListOf<LocationData>()
        
        val cursor = db.query(
            TABLE_LOCATIONS,
            null,
            "$COLUMN_SYNCED = ?",
            arrayOf("0"),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val location = LocationData(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    deviceId = it.getString(it.getColumnIndexOrThrow(COLUMN_DEVICE_ID)),
                    latitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    accuracy = it.getFloat(it.getColumnIndexOrThrow(COLUMN_ACCURACY)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE)),
                    time = it.getString(it.getColumnIndexOrThrow(COLUMN_TIME)),
                    synced = it.getInt(it.getColumnIndexOrThrow(COLUMN_SYNCED)) == 1
                )
                locations.add(location)
            }
        }
        
        Log.d(TAG, "Retrieved ${locations.size} unsynced location records")
        return locations
    }
    
    fun getAllLocationData(): List<LocationData> {
        val db = readableDatabase
        val locations = mutableListOf<LocationData>()
        
        val cursor = db.query(
            TABLE_LOCATIONS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val location = LocationData(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    deviceId = it.getString(it.getColumnIndexOrThrow(COLUMN_DEVICE_ID)),
                    latitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    accuracy = it.getFloat(it.getColumnIndexOrThrow(COLUMN_ACCURACY)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE)),
                    time = it.getString(it.getColumnIndexOrThrow(COLUMN_TIME)),
                    synced = it.getInt(it.getColumnIndexOrThrow(COLUMN_SYNCED)) == 1
                )
                locations.add(location)
            }
        }
        
        Log.d(TAG, "Retrieved ${locations.size} total location records")
        return locations
    }
    
    fun getLocationDataByDate(date: String): List<LocationData> {
        val db = readableDatabase
        val locations = mutableListOf<LocationData>()
        
        val cursor = db.query(
            TABLE_LOCATIONS,
            null,
            "$COLUMN_DATE = ?",
            arrayOf(date),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val location = LocationData(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    deviceId = it.getString(it.getColumnIndexOrThrow(COLUMN_DEVICE_ID)),
                    latitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    accuracy = it.getFloat(it.getColumnIndexOrThrow(COLUMN_ACCURACY)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE)),
                    time = it.getString(it.getColumnIndexOrThrow(COLUMN_TIME)),
                    synced = it.getInt(it.getColumnIndexOrThrow(COLUMN_SYNCED)) == 1
                )
                locations.add(location)
            }
        }
        
        Log.d(TAG, "Retrieved ${locations.size} location records for date: $date")
        return locations
    }
    
    fun markLocationAsSynced(locationId: Long): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SYNCED, 1)
        }
        
        val rowsUpdated = db.update(
            TABLE_LOCATIONS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(locationId.toString())
        )
        
        val success = rowsUpdated > 0
        Log.d(TAG, "Location ID $locationId marked as synced: $success")
        return success
    }
    
    fun markMultipleLocationsAsSynced(locationIds: List<Long>): Int {
        val db = writableDatabase
        var updatedCount = 0
        
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_SYNCED, 1)
            }
            
            for (locationId in locationIds) {
                val rowsUpdated = db.update(
                    TABLE_LOCATIONS,
                    values,
                    "$COLUMN_ID = ?",
                    arrayOf(locationId.toString())
                )
                if (rowsUpdated > 0) updatedCount++
            }
            
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking multiple locations as synced", e)
        } finally {
            db.endTransaction()
        }
        
        Log.d(TAG, "Marked $updatedCount locations as synced")
        return updatedCount
    }
    
    fun deleteOldLocationData(daysToKeep: Int = 30): Int {
        val db = writableDatabase
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        
        // Delete ALL location data older than specified days (both synced and unsynced)
        val deletedRows = db.delete(
            TABLE_LOCATIONS,
            "$COLUMN_TIMESTAMP < ?",
            arrayOf(cutoffTime.toString())
        )
        
        Log.d(TAG, "Deleted $deletedRows old location records (older than $daysToKeep days)")
        return deletedRows
    }
    
    fun deleteOldestDayLocationData(): Int {
        val db = writableDatabase
        
        // Get the oldest date in the database
        val cursor = db.rawQuery(
            "SELECT $COLUMN_DATE FROM $TABLE_LOCATIONS ORDER BY $COLUMN_TIMESTAMP ASC LIMIT 1",
            null
        )
        
        var deletedRows = 0
        cursor.use {
            if (it.moveToFirst()) {
                val oldestDate = it.getString(0)
                
                // Delete all records from the oldest date
                deletedRows = db.delete(
                    TABLE_LOCATIONS,
                    "$COLUMN_DATE = ?",
                    arrayOf(oldestDate)
                )
                
                Log.d(TAG, "Deleted $deletedRows location records from oldest date: $oldestDate")
            }
        }
        
        return deletedRows
    }
    
    fun getDistinctDatesCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(DISTINCT $COLUMN_DATE) FROM $TABLE_LOCATIONS",
            null
        )
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
    
    fun getLocationCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_LOCATIONS", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
    
    fun getUnsyncedLocationCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_LOCATIONS WHERE $COLUMN_SYNCED = 0", 
            null
        )
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
    
    fun clearAllLocationData(): Int {
        val db = writableDatabase
        val deletedRows = db.delete(TABLE_LOCATIONS, null, null)
        Log.d(TAG, "Cleared all location data: $deletedRows records deleted")
        return deletedRows
    }
}