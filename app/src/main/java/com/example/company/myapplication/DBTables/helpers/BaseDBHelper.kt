package com.example.company.myapplication.DBTables.helpers

import android.content.Context
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.TrainingDataDao
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.TrainingSlideDataDao
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase

class BaseDBHelper {
    private val db: SpeechDataBase
    private val presentationDataDao: PresentationDataDao
    private val trainingDataDao: TrainingDataDao
    private val trainingSlideDataDao: TrainingSlideDataDao

    constructor(ctx: Context) {
        db = SpeechDataBase.getInstance(ctx)!!
        presentationDataDao = db.PresentationDataDao()
        trainingDataDao = db.TrainingDataDao()
        trainingSlideDataDao = db.TrainingSlideDataDao()
    }

}