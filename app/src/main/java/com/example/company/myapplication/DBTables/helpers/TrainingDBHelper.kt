package com.example.company.myapplication.DBTables.helpers

import android.content.Context
import android.util.Log
import com.example.company.myapplication.TEST_DB
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.example.putkovdimi.trainspeech.DBTables.TrainingData

class trainingHelper {
    companion object {
        fun addTrainingInDB(trainingData: TrainingData, presentationData: PresentationData, ctx: Context) {
            val presentationDataDao = SpeechDataBase.getInstance(ctx)?.PresentationDataDao()
            val trainingDataDao = SpeechDataBase.getInstance(ctx)?.TrainingDataDao()

            trainingDataDao?.insert(trainingData)
            val td = trainingDataDao?.getLastTraining()

            presentationData.trainingDataId = td?.id
            presentationDataDao?.updatePresentation(presentationData)

            Log.d(TEST_DB, "trainHelp pres: $presentationData")
        }


        //fun addTrainingInDB(trainingData: TrainingData, presentationData: PresentationData) {
            //trainingData?.timeStampInSec = System.currentTimeMillis() / 1000
            //trainingData?.allRecognizedText = ALL_RECOGNIZED_TEXT

            /*trainingDataDao?.insert(trainingData!!)
            trainingData = trainingDataDao?.getLastTraining()

            if (presentationData?.trainingDataId != null) {
                var trainingDataPtr = trainingDataDao?.getTrainingWithId(presentationData?.trainingDataId!!)
                while (trainingDataPtr?.nextTrainingId != null) trainingDataPtr = trainingDataDao?.getTrainingWithId(trainingDataPtr.nextTrainingId!!)

                trainingDataPtr?.nextTrainingId = trainingData?.id
                trainingDataDao?.updateTraining(trainingDataPtr!!)
            }
            else {
                presentationData?.trainingDataId = trainingData?.id
                presentationDataDao?.updatePresentation(presentationData!!)
            }


            Log.d(TEST_DB, "training act: addTraining in db")
        }*/

    }
}