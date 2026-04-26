package com.hubble.openbrain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.hubble.openbrain.MainActivity
import com.hubble.openbrain.R

object CaptureNotifications {

    const val CHANNEL_ID = "capture"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Capture",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shown while Open Brain is listening"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun build(context: Context, contentText: String): NotificationCompat.Builder {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            context,
            0,
            Intent(context, CaptureService::class.java).setAction(CaptureService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mic)
            .setContentTitle("Open Brain · Capturing")
            .setContentText(contentText)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
    }
}
