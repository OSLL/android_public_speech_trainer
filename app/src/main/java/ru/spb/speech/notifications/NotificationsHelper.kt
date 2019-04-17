package ru.spb.speech.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import ru.spb.speech.DBTables.SpeechDataBase
import ru.spb.speech.DBTables.TrainingData
import ru.spb.speech.DBTables.helpers.TrainingDBHelper
import ru.spb.speech.DBTables.helpers.TrainingSlideDBHelper
import ru.spb.speech.R
import ru.spb.speech.StartPageActivity
import java.text.SimpleDateFormat
import java.util.*

class NotificationsHelper(private val context: Context) {

    private val CHANNEL_ID = "1011"
    private val NOTIFICATION_ID = 15

    private val FINISHED_TRAININGS = 5

    companion object {
        val HOUR_FOR_NOTIFICATION = 20
        val MINUTES_FOR_NOTIFICATION = 30
    }

    init {
        createNotificationChannel(context)
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

    fun sendNotification() {
        val intent = Intent(context, StartPageActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(context.getString(R.string.notifications_title))
                .setContentText(context.getString(R.string.notifications_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    fun validateNotification(finishedTrainings: Int = FINISHED_TRAININGS): Boolean {
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

                var endedTrainings = 0
                for (training in list) {
                    if (isTrainingEnd(training, presentation.pageCount!!))
                        endedTrainings += 1
                }

                if (endedTrainings < finishedTrainings && trainingDate.before(dayBefore) && Date().before(presentationDate)) {
                    return true
                }
            }
            else {
                if (Date().before(presentationDate)) {
                    return true
                }
            }
        }

        return false
    }

    private fun isTrainingEnd(training: TrainingData, slidesCount: Int): Boolean {
        val helper = TrainingSlideDBHelper(context)
        val slides = helper.getAllSlidesForTraining(training)

        if (slides != null && slides.count() < slidesCount)
            return false
        return true
    }
}