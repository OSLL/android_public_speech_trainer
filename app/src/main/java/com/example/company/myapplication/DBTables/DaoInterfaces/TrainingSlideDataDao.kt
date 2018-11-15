package com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.example.putkovdimi.trainspeech.DBTables.TrainingSlideData

@Dao
interface TrainingSlideDataDao {

    @Query("SELECT * from trainingslidedata")
    fun getAll(): List<TrainingSlideData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trainingsSlideData: TrainingSlideData)

    @Query("DELETE from trainingslidedata")
    fun deleteAll()
}