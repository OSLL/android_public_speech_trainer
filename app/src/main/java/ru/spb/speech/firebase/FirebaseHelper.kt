package ru.spb.speech.firebase

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase
import ru.spb.speech.database.PresentationData
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.helpers.TrainingDBHelper
import ru.spb.speech.database.helpers.TrainingSlideDBHelper
import ru.spb.speech.R
import ru.spb.speech.appSupport.TrainingStatisticsData
import ru.spb.speech.firebase.model.FullTrainingStatistic
import java.util.*
import java.util.concurrent.TimeUnit

const val firebaseLog = "FIREBASE_LOG"

class FirebaseHelper(private val context: Context) {
    @SuppressLint("HardwareIds")
    private val androidID = Settings.Secure.getString(context.contentResolver,
            Settings.Secure.ANDROID_ID)

    private val firebaseDB = FirebaseDatabase.getInstance()
    private val trainingDBHelper: TrainingDBHelper = TrainingDBHelper(context)
    private val trainingSlideDBHelper: TrainingSlideDBHelper = TrainingSlideDBHelper(context)
    private val db: SpeechDataBase = SpeechDataBase.getInstance(context)!!

    suspend fun registerNewTester() {
        FirebaseAnalytics.getInstance(context).setUserId(androidID)
    }

    suspend fun uploadLastTraining(presentationData: PresentationData?) {
        if (presentationData == null) return

        this.updatePresentationStatistic(getFullTrainingStatistic(presentationData), presentationData.id)

        val lastTraining = db.TrainingDataDao().getLastTraining()

        firebaseDB
                .getReference("/testers/$androidID/${presentationData.id}/trainings/${lastTraining.id}")
                .setValue(lastTraining)

        val slides = trainingSlideDBHelper.getAllSlidesForTraining(lastTraining) ?: return

        for (slide in slides) {
            firebaseDB
                    .getReference("/testers/$androidID/${presentationData.id}/trainings/${lastTraining.id}/slides/${slide.id}")
                    .setValue(slide)
        }
    }

    suspend fun synchronizeAllData() {
        for (presentation in db.PresentationDataDao().getAll()) {
            val trainings = trainingDBHelper.getAllTrainingsForPresentation(presentation)
                    ?: continue

            this.synchronizePresentationStatistic(getFullTrainingStatistic(presentation))

            for (training in trainings) {
                val slides = trainingSlideDBHelper.getAllSlidesForTraining(training) ?: continue

                firebaseDB
                        .getReference("/testers/$androidID/${presentation.id}/trainings/${training.id}")
                        .setValue(training)

                for (slide in slides) {
                    firebaseDB
                            .getReference("/testers/$androidID/${presentation.id}/trainings/${training.id}/slides/${slide.id}")
                            .setValue(slide)
                }
            }
        }
    }

    private fun updatePresentationStatistic(fts: FullTrainingStatistic, presentationId: Int?) {
        val presRef = firebaseDB.getReference("/testers/$androidID/$presentationId")

        presRef.updateChildren(mapOf(Pair("averageDeviationLimitRestriction", fts.averageDeviationLimitRestriction)))
        presRef.updateChildren(mapOf(Pair("averageTrainingTime", fts.averageTrainingTime)))
        presRef.updateChildren(mapOf(Pair("average_min_maxMarks", fts.average_min_maxMarks)))
        presRef.updateChildren(mapOf(Pair("copedCount_allCount", fts.copedCount_allCount)))
        presRef.updateChildren(mapOf(Pair("countOfAllWords", fts.countOfAllWords)))
        presRef.updateChildren(mapOf(Pair("finishedTrainings_allTrainings", fts.finishedTrainings_allTrainings)))
        presRef.updateChildren(mapOf(Pair("lastTrainingDate", fts.lastTrainingDate)))
        presRef.updateChildren(mapOf(Pair("maxTrainingTime", fts.maxTrainingTime)))
        presRef.updateChildren(mapOf(Pair("minTrainingTime", fts.minTrainingTime)))
        presRef.updateChildren(mapOf(Pair("presentationName", fts.presentationName)))
    }

    private fun synchronizePresentationStatistic(fts: FullTrainingStatistic) {
        firebaseDB
                .getReference("/testers/$androidID/${fts.trainingID}")
                .setValue(fts)
                .addOnSuccessListener { Log.d(firebaseLog, "success update: $fts") }
                .addOnFailureListener { Log.d(firebaseLog, "failed update (e: $it): $fts") }
    }

    private fun getFullTrainingStatistic(pd: PresentationData): FullTrainingStatistic {
        val trainingStatisticsData = TrainingStatisticsData(context, pd, db.TrainingDataDao().getLastTraining())

        val fts = FullTrainingStatistic(pd.id!!)
        fts.presentationName = "${trainingStatisticsData.presName}"
        fts.firstTrainingDate = trainingStatisticsData.dateOfFirstTraining
        fts.lastTrainingDate = trainingStatisticsData.dateOfCurTraining
        fts.finishedTrainings_allTrainings = "${trainingStatisticsData.countOfCompleteTraining} / ${trainingStatisticsData.trainingCount}"
        fts.copedCount_allCount = "${trainingStatisticsData.fallIntoReg} / ${trainingStatisticsData.trainingCount}"
        fts.averageDeviationLimitRestriction = getStringPresentationTimeLimit(trainingStatisticsData.averageExtraTime)
        fts.maxTrainingTime = getStringPresentationTimeLimit(trainingStatisticsData.maxTrainTime)
        fts.minTrainingTime = getStringPresentationTimeLimit(trainingStatisticsData.minTrainTime)
        fts.averageTrainingTime = getStringPresentationTimeLimit(trainingStatisticsData.averageTime)
        fts.countOfAllWords = " ${trainingStatisticsData.allWords}"
        fts.average_min_maxMarks = "${trainingStatisticsData.averageEarn.toInt()} / ${trainingStatisticsData.minEarn.toInt()} / ${trainingStatisticsData.maxEarn.toInt()}"
        return fts
    }

    @SuppressLint("UseSparseArrays")
    private fun getStringPresentationTimeLimit(t: Long?): String {

        if (t == null)
            return "undefined"

        var millisUntilFinishedVar: Long = t


        val minutes = TimeUnit.SECONDS.toMinutes(millisUntilFinishedVar)
        millisUntilFinishedVar -= TimeUnit.MINUTES.toSeconds(minutes)

        val seconds = millisUntilFinishedVar

        return String.format(
                Locale.getDefault(),
                " %02d:%02d",
                minutes, seconds
        )

    }
}