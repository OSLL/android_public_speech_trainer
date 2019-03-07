package ru.spb.speech.DBTables.DaoInterfaces

import android.arch.persistence.room.*
import ru.spb.speech.DBTables.TrainingData

@Dao
interface TrainingDataDao {

    @Query("SELECT * from trainingdata")
    fun getAll(): List<TrainingData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trainingData: TrainingData)

    @Query("DELETE from trainingdata")
    fun deleteAll()

    @Query("DELETE FROM trainingdata WHERE id = :ID")
    fun deleteTrainingWithId(ID: Int)

    @Query("SELECT * from trainingdata WHERE id = :ID LIMIT 1")
    fun getTrainingWithId(ID: Int): TrainingData

    @Update
    fun updateTraining(trainingData: TrainingData)

    @Query("SELECT * from trainingdata WHERE id = (SELECT MAX(id) from trainingdata)")
    fun getLastTraining(): TrainingData

}