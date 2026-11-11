package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_SERVICE_ID = "channel_telegram_upload_service"
    const val CHANNEL_ALERTS_ID = "channel_telegram_upload_alerts"
    const val SERVICE_NOTIFICATION_ID = 1001
    const val EVENT_NOTIFICATION_ID = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "سرویس ارسال خودکار به تلگرام",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش وضعیت سرویس پس‌زمینه و عکس‌های در انتظار"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "اعلان‌های ارسال عکس",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "اعلان وضعیت ارسال و خطاهای آپلود عکس"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    fun buildServiceNotification(
        context: Context,
        pendingCount: Int = 0,
        sentCount: Int = 0,
        currentStatus: String? = null
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            if (currentStatus != null) {
                append("$currentStatus • ")
            }
            append("در انتظار: $pendingCount | ارسال شده: $sentCount")
        }

        return NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
            .setContentTitle("ارسال خودکار عکس به تلگرام فعال است")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
