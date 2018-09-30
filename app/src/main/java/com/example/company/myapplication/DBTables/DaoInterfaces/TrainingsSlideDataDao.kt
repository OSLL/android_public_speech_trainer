package com.example.company.myapplication.DBTables.DaoInterfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.example.company.myapplication.DBTables.TrainingsSlideData

@Dao
interface TrainingsSlideDataDao {

    @Query("SELECT * from trainingsslidedata")
    fun getAll(): List<TrainingsSlideData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trainingsSlideData: TrainingsSlideData)

    @Query("DELETE from trainingsslidedata")
    fun deleteAll()
}