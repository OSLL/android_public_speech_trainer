package ru.spb.speech.DBTables

import android.arch.persistence.room.*

@Entity(tableName = "TrainingData")
data class TrainingData(@PrimaryKey(autoGenerate = true) var id: Int?,
                        @ColumnInfo(name = "timeStampInSec") var timeStampInSec: Long?,
                        @ColumnInfo(name = "allRecognizedText") var allRecognizedText: String,
                        @ColumnInfo(name = "nextTrainingId") var nextTrainingId: Int?,
                        @ColumnInfo(name = "trainingSlideId") var trainingSlideId: Int?,
                        @ColumnInfo(name = "exerciseTimeFactorMarkX") var exerciseTimeFactorMarkX: String?,
                        @ColumnInfo(name = "speechSpeedFactorMarkY") var speechSpeedFactorMarkY: String?,
                        @ColumnInfo(name = "timeOnSlidesFactorMarkZ") var timeOnSlidesFactorMarkZ: String?
){
    constructor():this(null,0,"",null,null, null, null, null)
}