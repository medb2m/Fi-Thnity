package tn.esprit.fithnity.services

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONObject
import tn.esprit.fithnity.data.NotificationResponse
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for receiving real-time notifications from server
 */
class NotificationWebSocketClient(private val authToken: String?) {
    private val TAG = "NotificationWebSocket"
    private val WS_BASE_URL = "ws://72.61.145.239:9090/ws/notifications"
    
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    
    private val _newNotification = MutableStateFlow<NotificationResponse?>(null)
    val newNotification: StateFlow<NotificationResponse?> = _newNotification
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError
    
    fun connect() {
        if (authToken == null) {
            Log.w(TAG, "Cannot connect: No auth token")
            _connectionError.value = "No authentication token"
            return
        }
        
        if (webSocket != null && _isConnected.value) {
            Log.d(TAG, "WebSocket already connected")
            return
        }
        
        okHttpClient = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
        
        // Add token as query parameter for authentication
        val wsUrl = "$WS_BASE_URL?token=$authToken"
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                Log.d(TAG, "Notification WebSocket connected")
                this@NotificationWebSocketClient.webSocket = webSocket
                _isConnected.value = true
                _connectionError.value = null
            }
            
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    
                    when (type) {
                        "connected" -> {
                            val message = json.optString("message", "Connected")
                            Log.d(TAG, "Notification WebSocket: $message")
                        }
                        "notification" -> {
                            val data = json.getJSONObject("data")
                            val notification = NotificationResponse(
                                _id = data.getString("_id"),
                                user = data.getString("user"),
                                type = data.getString("type"),
                                title = data.getString("title"),
                                message = data.getString("message"),
                                data = parseDataObject(data.optJSONObject("data")),
                                read = data.optBoolean("read", false),
                                readAt = data.optString("readAt", null),
                                createdAt = data.optString("createdAt", null),
                                updatedAt = data.optString("updatedAt", null),
                                timeAgo = null
                            )
                            
                            Log.d(TAG, "Received new notification: ${notification.title}")
                            _newNotification.value = notification
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing notification message", e)
                }
            }
            
            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Notification WebSocket closing: $code - $reason")
                _isConnected.value = false
            }
            
            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Notification WebSocket closed: $code - $reason")
                this@NotificationWebSocketClient.webSocket = null
                _isConnected.value = false
            }
            
            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Notification WebSocket failure", t)
                this@NotificationWebSocketClient.webSocket = null
                _isConnected.value = false
                _connectionError.value = t.message ?: "Connection failed"
            }
        }
        
        webSocket = okHttpClient!!.newWebSocket(request, listener)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _isConnected.value = false
    }
    
    fun sendMessage(message: String) {
        webSocket?.send(message) ?: Log.e(TAG, "WebSocket not connected, cannot send message")
    }
    
    /**
     * Parse JSONObject data to Map<String, Any>
     */
    private fun parseDataObject(dataObj: org.json.JSONObject?): Map<String, Any>? {
        if (dataObj == null) return null
        
        val map = mutableMapOf<String, Any>()
        val keys = dataObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = dataObj.get(key)
            map[key] = when (value) {
                is org.json.JSONObject -> parseDataObject(value) ?: emptyMap<String, Any>()
                is org.json.JSONArray -> value.toString() // Convert array to string for simplicity
                else -> value
            }
        }
        return map
    }
}

