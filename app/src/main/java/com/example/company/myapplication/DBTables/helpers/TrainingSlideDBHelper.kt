package com.example.company.myapplication.DBTables.helpers

import android.content.Context
import android.util.Log
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.example.putkovdimi.trainspeech.DBTables.TrainingData
import com.example.putkovdimi.trainspeech.DBTables.TrainingSlideData

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