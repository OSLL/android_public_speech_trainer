package ru.spb.speech.DBTables.helpers

import android.content.Context
import ru.spb.speech.DBTables.SpeechDataBase
import ru.spb.speech.DBTables.TrainingData
import ru.spb.speech.DBTables.TrainingSlideData

class TrainingSlideDBHelper {
    private var context: Context

    constructor(context: Context) {
        this.context = context
    }

    fun addTrainingSlideInDB(tsd: TrainingSlideData, trainingData: TrainingData) {
        val trainingDataDao = SpeechDataBase.getInstance(context)?.TrainingDataDao()
        val trainingSlideDataDao = SpeechDataBase.getInstance(context)?.TrainingSlideDataDao()

        trainingSlideDataDao?.insert(tsd)
        val trainingSlideData = trainingSlideDataDao?.getLastSlide()

        if (trainingData.trainingSlideId != null) {
            var trainingSlideDataPtr = trainingSlideDataDao?.getSlideWithId(trainingData?.trainingSlideId!!)
            while (trainingSlideDataPtr?.nextSlideId != null) trainingSlideDataPtr = trainingSlideDataDao?.getSlideWithId(trainingSlideDataPtr.nextSlideId!!)

            trainingSlideDataPtr?.nextSlideId = trainingSlideData?.id
            trainingSlideDataDao?.updateSlide(trainingSlideDataPtr!!)
        }
        else {
            trainingData.trainingSlideId = trainingSlideData?.id
            trainingDataDao?.updateTraining(trainingData)
        }
    }

    fun getAllSlidesForTraining(trainingData: TrainingData): MutableList<TrainingSlideData>? {
        if (trainingData.trainingSlideId == null) return null

        val trainingSlideDataDao = SpeechDataBase.getInstance(context)?.TrainingSlideDataDao()
        val list: MutableList<TrainingSlideData> = mutableListOf()

        var trainingSlideDataPtr = trainingSlideDataDao?.getSlideWithId(trainingData.trainingSlideId!!)
        list.add(trainingSlideDataPtr!!)

        while (trainingSlideDataPtr?.nextSlideId != null) {
            trainingSlideDataPtr = trainingSlideDataDao?.getSlideWithId(trainingSlideDataPtr?.nextSlideId!!)
            list.add(trainingSlideDataPtr!!)
        }
        return list
    }
}