package com.ru.putkovdimi.trainspeech.DBTables

import android.arch.persistence.room.*

@Entity(tableName = "PresentationData")
data class PresentationData(@PrimaryKey(autoGenerate = true) var id: Int?,
                            @ColumnInfo(name = "name") var name: String,
                            @ColumnInfo(name = "stringUri") var stringUri: String,
                            @ColumnInfo(name = "timeLimit") var timeLimit : Long?,
                            @ColumnInfo(name = "pageCount") var pageCount: Int?,
                            @ColumnInfo(name = "debugPresentationFlag") var debugFlag: Int,
                            @ColumnInfo(name = "trainingDataId") var trainingDataId: Int?

){
    constructor():this(null,"","",null,0,0, null)
}