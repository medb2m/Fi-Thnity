package tn.esprit.fithnity.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import tn.esprit.fithnity.MainActivity
import tn.esprit.fithnity.R

object NotificationHelper {
    private const val CHANNEL_ID = "fithnity_notifications"
    private const val CHANNEL_NAME = "FiThnity Notifications"
    
    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for messages, comments, and public transport"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show notification
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        type: String,
        data: Map<String, Any>?
    ) {
        createNotificationChannel(context)
        
        // Create intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add notification data for navigation
            putExtra("notification_type", type)
            data?.forEach { (key, value) ->
                when (value) {
                    is String -> putExtra(key, value)
                    is Int -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                    is Double -> putExtra(key, value)
                    is Float -> putExtra(key, value)
                }
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Choose icon based on notification type
        val icon = when (type) {
            "MESSAGE" -> android.R.drawable.ic_dialog_email
            "COMMENT" -> android.R.drawable.ic_menu_info_details
            "PUBLIC_TRANSPORT_SEARCH" -> android.R.drawable.ic_menu_compass
            else -> android.R.drawable.ic_dialog_info
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
        
        android.util.Log.d("NotificationHelper", "Showed notification: $title - $message")
    }
}

