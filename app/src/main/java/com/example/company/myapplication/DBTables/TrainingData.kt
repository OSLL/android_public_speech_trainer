package com.example.putkovdimi.trainspeech.DBTables

import android.arch.persistence.room.*
import android.support.annotation.ColorInt

@Entity(tableName = "TrainingData")
data class TrainingData(@PrimaryKey(autoGenerate = true) var id: Int?,
                        @ColumnInfo(name = "timeStampInSec") var timeStampInSec: Long?,
                        @ColumnInfo(name = "allRecognizedText") var allRecognizedText: String,
                        @ColumnInfo(name = "nextTrainingId") var nextTrainingId: Int?
){
    constructor():this(null,0,"",null)
}