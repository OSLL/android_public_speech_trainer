package ru.spb.speech.DBTables.helpers

import android.content.Context
import ru.spb.speech.DBTables.PresentationData
import ru.spb.speech.DBTables.SpeechDataBase
import ru.spb.speech.DBTables.TrainingData

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

    fun getAllTrainingsForPresentation(presentationData: PresentationData): MutableList<TrainingData>? {
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