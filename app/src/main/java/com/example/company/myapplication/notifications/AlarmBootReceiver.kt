package com.example.company.myapplication.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmBootReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("MyNotifications", "AlarmBootReceiver ${intent?.action}")

        if (intent?.action == "android.intent.action.BOOT_COMPLETED" ||
                intent?.action == "android.intent.action.QUICKBOOT_POWERON" ||
                intent?.action == "android.intent.action.REBOOT") {

            Log.d("MyNotifications", "notification init")
            initAlarm(context)

        }
    }

    private fun initAlarm(context: Context?) {

        val alarmIntent = Intent(context, AlarmReceiver::class.java).let {intent ->
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                1000,
                alarmIntent
        )
    }

}