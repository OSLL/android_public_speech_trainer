package com.example.company.myapplication.DBTables.helpers

import android.content.Context
import android.util.Log
import com.example.company.myapplication.TEST_DB
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.example.putkovdimi.trainspeech.DBTables.TrainingData

class TrainingDBHelper {

    private var context: Context

    constructor(context: Context) {
        this.context = context
    }

    fun addTrainingInDB(td: TrainingData, presentationData: PresentationData) {
        val presentationDataDao = SpeechDataBase.getInstance(context)?.PresentationDataDao()
        val trainingDataDao = SpeechDataBase.getInstance(context)?.TrainingDataDao()

        trainingDataDao?.insert(td)
        val trainingData = trainingDataDao?.getLastTraining()

        if (presentationData.trainingDataId != null) {
            var trainingDataPtr = trainingDataDao?.getTrainingWithId(presentationData.trainingDataId!!)
            while (trainingDataPtr?.nextTrainingId != null) trainingDataPtr = trainingDataDao?.getTrainingWithId(trainingDataPtr.nextTrainingId!!)

            trainingDataPtr?.nextTrainingId = trainingData?.id
            trainingDataDao?.updateTraining(trainingDataPtr!!)
        }
        else {
            presentationData.trainingDataId = trainingData?.id
            presentationDataDao?.updatePresentation(presentationData)
        }
    }

    fun getAllTrainings(presentationData: PresentationData): MutableList<TrainingData>? {
        if (presentationData.trainingDataId == null) return null

        val trainingDataDao = SpeechDataBase.getInstance(context)?.TrainingDataDao()
        val list: MutableList<TrainingData> = mutableListOf()

        var trainingDataPtr = trainingDataDao?.getTrainingWithId(presentationData.trainingDataId!!)
        list.add(trainingDataPtr!!)

        while (trainingDataPtr?.nextTrainingId != null) {
            trainingDataPtr = trainingDataDao?.getTrainingWithId(trainingDataPtr?.nextTrainingId!!)
            list.add(trainingDataPtr!!)
        }
        return list
    }

}