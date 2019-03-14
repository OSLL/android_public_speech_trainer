package com.example.company.myapplication.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.*

class AlarmBootReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == "android.intent.action.BOOT_COMPLETED" ||
                intent?.action == "android.intent.action.QUICKBOOT_POWERON" ||
                intent?.action == "android.intent.action.REBOOT") {

            initAlarm(context)

        }
    }

    private fun initAlarm(context: Context?) {
        val notificationTime = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
        }


        val alarmIntent = Intent(context, AlarmReceiver::class.java).let {intent ->
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                notificationTime.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                alarmIntent
        )
    }

}