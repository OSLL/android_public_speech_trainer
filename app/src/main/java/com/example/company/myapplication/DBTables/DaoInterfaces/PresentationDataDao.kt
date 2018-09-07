package com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.example.putkovdimi.trainspeech.DBTables.PresentationData


@Dao
interface PresentationDataDao {

    @Query("SELECT * from presentationdata")
    fun getAll(): List<PresentationData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(presentationData: PresentationData)

    @Query("DELETE from presentationdata")
    fun deleteAll()
}