package com.example.putkovdimi.trainspeech.DBTables

import android.arch.persistence.room.*

@Entity(tableName = "TrainingSlideData")
data class TrainingSlideData(@PrimaryKey(autoGenerate = true) var id:Int?,
                             @ColumnInfo(name = "trainingID") var trainingID: Int,
                             @ColumnInfo(name = "knownWords") var knownWords: String?,
                             @ColumnInfo(name = "slideDuration") var slideDuration : Int?
){
    constructor():this(null,0,null,null)
}