package tn.esprit.fithnity.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import tn.esprit.fithnity.MainActivity
import tn.esprit.fithnity.R
import tn.esprit.fithnity.data.VehicleLocationUpdate
import tn.esprit.fithnity.data.VehicleType
import java.util.concurrent.TimeUnit

/**
 * Foreground service for tracking and sharing vehicle location in real-time
 */
class VehicleLocationService : Service() {
    private val TAG = "VehicleLocationService"
    
    private var isTracking = false
    private var vehicleId: String? = null
    private var vehicleType: VehicleType = VehicleType.CAR
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vehicle_location_channel"
        private const val WS_URL = "ws://72.61.145.239:9090/ws/vehicle-location"
        
        const val ACTION_START_TRACKING = "tn.esprit.fithnity.START_TRACKING"
        const val ACTION_STOP_TRACKING = "tn.esprit.fithnity.STOP_TRACKING"
        const val EXTRA_VEHICLE_ID = "vehicle_id"
        const val EXTRA_VEHICLE_TYPE = "vehicle_type"
    }
    
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                vehicleId = intent.getStringExtra(EXTRA_VEHICLE_ID) ?: generateVehicleId()
                val typeString = intent.getStringExtra(EXTRA_VEHICLE_TYPE) ?: "CAR"
                vehicleType = try {
                    VehicleType.valueOf(typeString)
                } catch (e: Exception) {
                    VehicleType.CAR
                }
                startTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun generateVehicleId(): String {
        val userId = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("user_id", null) ?: "unknown"
        return "vehicle_${userId}_${System.currentTimeMillis()}"
    }
    
    private fun startTracking() {
        if (isTracking) return
        
        isTracking = true
        startForeground(NOTIFICATION_ID, createNotification("Sharing location..."))
        connectWebSocket()
        startLocationUpdates()
    }
    
    private fun stopTracking() {
        if (!isTracking) return
        
        isTracking = false
        stopLocationUpdates()
        disconnectWebSocket()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    
    private fun connectWebSocket() {
        okHttpClient = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
        
        val request = Request.Builder()
            .url(WS_URL)
            .build()
        
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                this@VehicleLocationService.webSocket = webSocket
                updateNotification("Connected - Sharing location")
            }
            
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                Log.d(TAG, "WebSocket message: $text")
                // Handle incoming messages if needed
            }
            
            override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) {
                Log.d(TAG, "WebSocket binary message")
            }
            
            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
            }
            
            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                this@VehicleLocationService.webSocket = null
            }
            
            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                this@VehicleLocationService.webSocket = null
                // Attempt to reconnect
                serviceScope.launch {
                    delay(5000)
                    if (isTracking) {
                        connectWebSocket()
                    }
                }
            }
        }
        
        okHttpClient?.newWebSocket(request, listener)
    }
    
    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Service stopped")
        webSocket = null
        okHttpClient = null
    }
    
    private fun sendLocationUpdate(location: Location) {
        val update = VehicleLocationUpdate(
            vehicleId = vehicleId ?: return,
            type = vehicleType.name,
            lat = location.latitude,
            lng = location.longitude,
            speed = location.speed * 3.6f, // Convert m/s to km/h
            bearing = location.bearing,
            timestamp = System.currentTimeMillis()
        )
        
        val json = JSONObject().apply {
            put("event", "update_location")
            put("data", JSONObject().apply {
                put("vehicleId", update.vehicleId)
                put("type", update.type)
                put("lat", update.lat)
                put("lng", update.lng)
                put("speed", update.speed)
                put("bearing", update.bearing)
                put("timestamp", update.timestamp)
            })
        }
        
        webSocket?.send(json.toString())
    }
    
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L // Update every 2 seconds
        ).apply {
            setMinUpdateIntervalMillis(1000L)
            setMaxUpdateDelayMillis(5000L)
        }.build()
        
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            stopTracking()
            return
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    sendLocationUpdate(location)
                    updateNotification("Location: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}")
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }
    
    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vehicle Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when vehicle location is being shared"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sharing Location")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
    }
}

