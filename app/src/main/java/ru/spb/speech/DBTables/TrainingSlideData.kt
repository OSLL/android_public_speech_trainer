package ru.spb.speech.DBTables

import android.arch.persistence.room.*

@Entity(tableName = "TrainingSlideData")
data class TrainingSlideData(@PrimaryKey(autoGenerate = true) var id:Int?,
                             @ColumnInfo(name = "spentTimeInSec") var spentTimeInSec: Long?,
                             @ColumnInfo(name = "knownWords") var knownWords: String?,
                             @ColumnInfo(name = "nextSlideId") var nextSlideId : Int?
){
    constructor():this(null,0,null,null)
}