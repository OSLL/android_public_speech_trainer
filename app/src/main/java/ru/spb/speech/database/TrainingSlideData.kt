package ru.spb.speech.database

import android.arch.persistence.room.*

@Entity(tableName = "TrainingSlideData")
data class TrainingSlideData(@PrimaryKey(autoGenerate = true) var id:Int?,
                             @ColumnInfo(name = "spentTimeInSec") var spentTimeInSec: Long?,
                             @ColumnInfo(name = "knownWords") var knownWords: String?,
                             @ColumnInfo(name = "nextSlideId") var nextSlideId : Int?,
                             @ColumnInfo(name = "silencePercentage") var silencePercentage: Double?,
                             @ColumnInfo(name = "pauseAverageLength") var pauseAverageLength: Int?,
                             @ColumnInfo(name = "longPausesAmount") var longPausesAmount: Int?
){
    constructor():this(null,0,null,null, null, null, null)
}