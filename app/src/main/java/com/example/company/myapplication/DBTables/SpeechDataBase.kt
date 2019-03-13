package com.example.putkovdimi.trainspeech.DBTables

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.example.company.myapplication.R
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.TrainingDataDao
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.TrainingSlideDataDao

@Database(entities = arrayOf(PresentationData::class, TrainingData::class, TrainingSlideData::class), version = 1)
abstract class SpeechDataBase : RoomDatabase() {

    abstract fun PresentationDataDao(): PresentationDataDao
    abstract fun TrainingDataDao(): TrainingDataDao
    abstract fun TrainingSlideDataDao(): TrainingSlideDataDao

    companion object {
        private var INSTANCE: SpeechDataBase? = null

        fun getInstance(context: Context): SpeechDataBase? {
            if (INSTANCE == null) {
                synchronized(SpeechDataBase::class) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            SpeechDataBase::class.java, context.getString(R.string.dbName))
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}