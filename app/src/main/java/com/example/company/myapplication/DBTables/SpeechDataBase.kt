package com.example.putkovdimi.trainspeech.DBTables

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.TrainingsDataDao
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.TrainingsSlideDataDao

@Database(entities = arrayOf(PresentationData::class, TrainingsData::class, TrainingsSlideData::class), version = 1)
abstract class SpeechDataBase : RoomDatabase() {

    abstract fun PresentationDataDao(): PresentationDataDao
    abstract fun TrainingsDataDao(): TrainingsDataDao
    abstract fun TrainingsSlideDataDao(): TrainingsSlideDataDao

    companion object {
        private var INSTANCE: SpeechDataBase? = null

        fun getInstance(context: Context): SpeechDataBase? {
            if (INSTANCE == null) {
                synchronized(SpeechDataBase::class) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            SpeechDataBase::class.java, "weather.0.1.db")
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