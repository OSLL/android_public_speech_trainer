package com.example.company.myapplication.DBTables.DaoInterfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.example.company.myapplication.DBTables.TrainingsData

@Dao
interface TrainingsDataDao {

    @Query("SELECT * from trainingsdata")
    fun getAll(): List<TrainingsData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trainingsSlideData: TrainingsData)

    @Query("DELETE from trainingsdata")
    fun deleteAll()
}