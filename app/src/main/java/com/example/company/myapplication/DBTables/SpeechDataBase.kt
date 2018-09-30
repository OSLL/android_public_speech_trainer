package com.example.company.myapplication.DBTables


import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import android.util.Log
import com.example.company.myapplication.DBTables.DaoInterfaces.PresentationDataDao
import com.example.company.myapplication.DBTables.DaoInterfaces.TrainingsDataDao
import com.example.company.myapplication.DBTables.DaoInterfaces.TrainingsSlideDataDao


@Database(entities = arrayOf(PresentationData::class, TrainingsData::class, TrainingsSlideData::class), version = 1)
abstract class SpeechDataBase : RoomDatabase() {

    abstract fun PresentationDataDao(): PresentationDataDao
    abstract fun TrainingsDataDao(): TrainingsDataDao
    abstract fun TrainingsSlideDataDao(): TrainingsSlideDataDao

    companion object {
        private var INSTANCE: SpeechDataBase? = null

        fun getInstance(context: Context): SpeechDataBase? {
            if (INSTANCE == null) {
                Log.d("In function", "MESSAGE")
                synchronized(SpeechDataBase::class) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            SpeechDataBase::class.java, "speechDB.db")
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