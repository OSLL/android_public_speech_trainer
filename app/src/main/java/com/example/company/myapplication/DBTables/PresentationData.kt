package com.example.putkovdimi.trainspeech.DBTables

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE

@Entity(tableName = "PresentationData")
data class PresentationData(@PrimaryKey(autoGenerate = true) var id: Int?,
                            @ColumnInfo(name = "name") var name: String,
                            @ColumnInfo(name = "path") var path: String,
                            @ColumnInfo(name = "timeLimit") var timeLimit : Int?
){
    constructor():this(null,"","",0)
}