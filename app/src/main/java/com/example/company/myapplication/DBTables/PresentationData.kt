package com.example.putkovdimi.trainspeech.DBTables

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE

@Entity(tableName = "PresentationData")
data class PresentationData(@PrimaryKey(autoGenerate = true) var id: Int?,
                            @ColumnInfo(name = "name") var name: String,
                            @ColumnInfo(name = "stringUri") var stringUri: String,
                            @ColumnInfo(name = "timeLimit") var timeLimit : Long?,
                            @ColumnInfo(name = "pageCount") var pageCount: Int?,
                            @ColumnInfo(name = "debugPresentationFlag") var debugFlag: Int
){
    constructor():this(null,"","",null,0,0)
}