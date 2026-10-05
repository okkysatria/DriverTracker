package com.example.drivertracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ServiceActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_START_TRACKING = "com.example.drivertracker.action.START_TRACKING"
        const val ACTION_PICKUP_CONFIRMED = "com.example.drivertracker.action.PICKUP_CONFIRMED"
        const val ACTION_COMPLETE_ORDER = "com.example.drivertracker.action.COMPLETE_ORDER"
        const val ACTION_CANCEL_ORDER = "com.example.drivertracker.action.CANCEL_ORDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val serviceIntent = Intent(context, TrackingService::class.java).apply {
            this.action = action
            intent.extras?.let { putExtras(it) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
