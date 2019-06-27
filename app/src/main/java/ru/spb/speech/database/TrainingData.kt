package ru.spb.speech.database

import android.arch.persistence.room.*

@Entity(tableName = "TrainingData")
data class TrainingData(@PrimaryKey(autoGenerate = true) var id: Int?,
                        @ColumnInfo(name = "timeStampInSec") var timeStampInSec: Long?,
                        @ColumnInfo(name = "allRecognizedText") var allRecognizedText: String,
                        @ColumnInfo(name = "nextTrainingId") var nextTrainingId: Int?,
                        @ColumnInfo(name = "trainingSlideId") var trainingSlideId: Int?,
                        @ColumnInfo(name = "audioPath") var audioPath: String,
                        @ColumnInfo(name = "audioFileName") var audioFileName: String
){
    constructor():this(null,0,"",null,null, "", "")
}