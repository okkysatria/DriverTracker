package com.example.drivertracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.drivertracker.MainActivity
import com.example.drivertracker.R
import com.example.drivertracker.data.repository.TrackingRepository
import com.example.drivertracker.data.repository.TrackingState
import java.util.Locale

object TrackingNotificationManager {

    const val CHANNEL_ID = "tracking_service_channel_v6"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Driver Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status pencatatan perjalanan"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        state: TrackingState,
        durationSeconds: Long,
        totalKm: Double,
        speedKmH: Float
    ): Notification {
        createNotificationChannel(context)

        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (state) {
            TrackingState.STARTED -> "Menuju lokasi jemput"
            TrackingState.PICKED_UP -> "Mengantar pesanan"
            TrackingState.IDLE -> "Driver Tracker • Standby"
        }

        val contentText = when (state) {
            TrackingState.STARTED, TrackingState.PICKED_UP -> {
                val hours = durationSeconds / 3600
                val minutes = (durationSeconds % 3600) / 60
                val seconds = durationSeconds % 60
                val timeFormatted = if (hours > 0) {
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }
                String.format(
                    Locale.getDefault(),
                    "%s • %.2f km • %.0f km/j",
                    timeFormatted, totalKm, speedKmH
                )
            }
            TrackingState.IDLE -> "Siap memulai pencatatan."
        }

        val compactLayout = RemoteViews(context.packageName, R.layout.notification_tracking_compact)
        val expandedLayout = RemoteViews(context.packageName, R.layout.notification_tracking)
        val notificationLayouts = listOf(compactLayout, expandedLayout)
        notificationLayouts.forEach { layout ->
            layout.setTextViewText(R.id.notif_title, title)
            layout.setTextViewText(R.id.notif_content, contentText)
            layout.setImageViewResource(R.id.notif_icon, R.mipmap.ic_launcher_round)
            layout.setViewVisibility(R.id.btn_action_2, View.GONE)
        }

        fun setActionButton(
            viewId: Int,
            label: String,
            pendingIntent: PendingIntent,
            visible: Boolean = true
        ) {
            notificationLayouts.forEach { layout ->
                layout.setTextViewText(viewId, label)
                layout.setViewVisibility(viewId, if (visible) View.VISIBLE else View.GONE)
                if (visible) layout.setOnClickPendingIntent(viewId, pendingIntent)
            }
        }

        fun broadcastPendingIntent(requestCode: Int, action: String, extras: Intent.() -> Unit = {}): PendingIntent {
            val intent = Intent(context, ServiceActionReceiver::class.java).apply {
                this.action = action
                extras()
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(state != TrackingState.IDLE)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCustomContentView(compactLayout)
            .setCustomBigContentView(expandedLayout)

        when (state) {
            TrackingState.IDLE -> {
                setActionButton(
                    R.id.btn_action_1,
                    "MULAI",
                    broadcastPendingIntent(5, ServiceActionReceiver.ACTION_START_TRACKING) {
                        putExtra("jenisOrder", "Penumpang")
                    }
                )
            }
            TrackingState.STARTED -> {
                setActionButton(
                    R.id.btn_action_1,
                    "JEMPUT",
                    broadcastPendingIntent(1, ServiceActionReceiver.ACTION_PICKUP_CONFIRMED)
                )
                setActionButton(
                    R.id.btn_action_2,
                    "BATAL",
                    broadcastPendingIntent(2, ServiceActionReceiver.ACTION_CANCEL_ORDER)
                )
            }
            TrackingState.PICKED_UP -> {
                val saveIntent = Intent(context, MainActivity::class.java).apply {
                    action = "ACTION_OPEN_SAVE_ORDER"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_save_dialog", true)
                }
                val savePendingIntent = PendingIntent.getActivity(
                    context, 3, saveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setActionButton(R.id.btn_action_1, "SELESAI", savePendingIntent)
                setActionButton(
                    R.id.btn_action_2,
                    "BATAL",
                    broadcastPendingIntent(4, ServiceActionReceiver.ACTION_CANCEL_ORDER)
                )
            }
        }

        return builder.build()
    }

    fun showNotification(
        context: Context,
        state: TrackingState = TrackingRepository.trackingState.value,
        durationSeconds: Long = TrackingRepository.durationSeconds.value,
        totalKm: Double = TrackingRepository.totalJarak.value,
        speedKmH: Float = TrackingRepository.currentSpeedKmH.value
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return
        }
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel(context)
            val notif = buildNotification(context, state, durationSeconds, totalKm, speedKmH)
            manager.notify(NOTIFICATION_ID, notif)
        } catch (_: Exception) {}
    }

    fun cancelNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
