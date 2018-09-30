package com.example.company.myapplication.DBTables.DaoInterfaces

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.example.company.myapplication.DBTables.PresentationData


@Dao
interface PresentationDataDao {

    @Query("SELECT * from presentationdata")
    fun getAll(): List<PresentationData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(presentationData: PresentationData)

    @Query("DELETE from presentationdata")
    fun deleteAll()
}