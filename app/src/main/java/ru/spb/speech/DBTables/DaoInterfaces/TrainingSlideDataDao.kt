package ru.spb.speech.DBTables.DaoInterfaces

import android.arch.persistence.room.*
import ru.spb.speech.DBTables.TrainingSlideData

@Dao
interface TrainingSlideDataDao {

    @Query("SELECT * from trainingslidedata")
    fun getAll(): List<TrainingSlideData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trainingsSlideData: TrainingSlideData)

    @Query("DELETE from trainingslidedata")
    fun deleteAll()

    @Query("DELETE FROM trainingslidedata WHERE id = :ID")
    fun deleteSlideWithId(ID: Int)

    @Query("SELECT * from trainingslidedata WHERE id = :ID LIMIT 1")
    fun getSlideWithId(ID: Int): TrainingSlideData

    @Update
    fun updateSlide(trainingsSlideData: TrainingSlideData)

    @Query("SELECT * from trainingslidedata WHERE id = (SELECT MAX(id) from trainingslidedata)")
    fun getLastSlide(): TrainingSlideData
}