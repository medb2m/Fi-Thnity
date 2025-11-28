package tn.esprit.fithnity.services

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import tn.esprit.fithnity.data.VehiclePosition
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for receiving vehicle position updates from server
 */
class VehicleWebSocketClient {
    private val TAG = "VehicleWebSocketClient"
    private val WS_URL = "ws://72.61.145.239:9090/ws/vehicle-location"
    
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    
    private val _vehiclePositions = MutableStateFlow<Map<String, VehiclePosition>>(emptyMap())
    val vehiclePositions: StateFlow<Map<String, VehiclePosition>> = _vehiclePositions
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    fun connect() {
        if (webSocket != null) {
            Log.d(TAG, "WebSocket already connected")
            return
        }
        
        okHttpClient = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
        
        val request = Request.Builder()
            .url(WS_URL)
            .build()
        
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                this@VehicleWebSocketClient.webSocket = webSocket
                _isConnected.value = true
                
                // Subscribe to receive vehicle positions
                val subscribeMessage = org.json.JSONObject().apply {
                    put("event", "subscribe")
                }
                webSocket.send(subscribeMessage.toString())
            }
            
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val event = json.optString("event")
                    
                    when (event) {
                        "vehicle_position" -> {
                            val data = json.getJSONObject("data")
                            val position = VehiclePosition(
                                vehicleId = data.getString("vehicleId"),
                                type = data.getString("type"),
                                lat = data.getDouble("lat"),
                                lng = data.getDouble("lng"),
                                speed = data.optDouble("speed", 0.0).toFloat(),
                                bearing = data.optDouble("bearing", 0.0).toFloat(),
                                timestamp = data.optLong("timestamp", System.currentTimeMillis()),
                                driverName = data.optString("driverName", null),
                                driverPhoto = data.optString("driverPhoto", null)
                            )
                            
                            // Update vehicle positions map
                            val currentPositions = _vehiclePositions.value.toMutableMap()
                            currentPositions[position.vehicleId] = position
                            _vehiclePositions.value = currentPositions
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message", e)
                }
            }
            
            override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) {
                Log.d(TAG, "WebSocket binary message")
            }
            
            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
            }
            
            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                this@VehicleWebSocketClient.webSocket = null
                _isConnected.value = false
            }
            
            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                this@VehicleWebSocketClient.webSocket = null
                _isConnected.value = false
            }
        }
        
        okHttpClient?.newWebSocket(request, listener)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        okHttpClient = null
        _isConnected.value = false
        _vehiclePositions.value = emptyMap()
    }
    
    fun removeVehicle(vehicleId: String) {
        val currentPositions = _vehiclePositions.value.toMutableMap()
        currentPositions.remove(vehicleId)
        _vehiclePositions.value = currentPositions
    }
}

