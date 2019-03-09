package com.example.company.myapplication.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.example.company.myapplication.DBTables.helpers.TrainingDBHelper
import com.example.company.myapplication.R
import com.example.company.myapplication.StartPageActivity
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.example.putkovdimi.trainspeech.DBTables.TrainingData
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver: BroadcastReceiver() {
    val CHANNEL_ID = "1011"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("MyNotifications", "AlarmReceiver")

        if (validateNotification(context!!)) {
            createNotificationChannel(context)
            sendNotification(context)
        }
    }

    private fun sendNotification(context: Context) {
        val intent = Intent(context, StartPageActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notifications_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(15, notificationBuilder.build())
        }
    }

    private fun validateNotification(context: Context): Boolean {
        val presentationsList = SpeechDataBase.getInstance(context)?.PresentationDataDao()?.getPresentationsWithEnabledNotifications()

        for (presentation in presentationsList!!) {
            val list = TrainingDBHelper(context).getAllTrainingsForPresentation(presentation)
            val presentationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(presentation.presentationDate)
            val dayBefore = with(Calendar.getInstance()) {
                this.time = Date()
                this.add(Calendar.DATE, -1)
                this.time
            }

            if (list != null) {
                val trainingDate = Date(list[list.size - 1].timeStampInSec!! * 1000)

                if (list.size < 5 && trainingDate.before(dayBefore) && Date().before(presentationDate))
                    return true
            }
            else {
                if (Date().before(presentationDate))
                    return true
            }
        }

        return false
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.about_text)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}