package com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces

import android.arch.persistence.room.*
import com.example.putkovdimi.trainspeech.DBTables.PresentationData


@Dao
interface PresentationDataDao {
    @Query("SELECT * from presentationdata")
    fun getAll(): List<PresentationData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(presentationData: PresentationData)

    @Query("DELETE from presentationdata")
    fun deleteAll()

    @Query("SELECT * from presentationdata WHERE id = :ID LIMIT 1")
    fun getPresentationWithId(ID: Int): PresentationData

    @Query("SELECT * from presentationdata WHERE name = :NAME LIMIT 1")
    fun getPresentationWithName(NAME: String): PresentationData

    @Update
    fun updatePresentation(presentationData: PresentationData)

    @Query("SELECT * from presentationdata WHERE stringUri = :strUri LIMIT 1")
    fun getPresentationDataWithUri(strUri: String): PresentationData

    @Query("SELECT * from presentationdata WHERE id = (SELECT MAX(id) from presentationdata)")
    fun getLastPresentation(): PresentationData
}