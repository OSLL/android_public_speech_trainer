package ru.spb.speech.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import ru.spb.speech.database.interfaces.PresentationDataDao
import ru.spb.speech.database.interfaces.TrainingDataDao
import ru.spb.speech.database.interfaces.TrainingSlideDataDao
import ru.spb.speech.R


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