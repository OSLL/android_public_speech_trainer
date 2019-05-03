package ru.spb.speech.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log

class AlarmReceiver: BroadcastReceiver() {
    val NOTIFICATIONS_TAG = "NOTIFICATIONS"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {

        try {
            val notificationsHelper = NotificationsHelper(context!!)
            if (notificationsHelper.validateNotification())
                notificationsHelper.sendNotification()

        } catch (e: Exception) {
            Log.d(NOTIFICATIONS_TAG, e.message)
        }

    }
}