package com.example.company.myapplication.DBTables

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE

@Entity(tableName = "PresentationData")
data class PresentationData(@PrimaryKey(autoGenerate = true) var id: Long?,
                            @ColumnInfo(name = "name") var name: String,
                            @ColumnInfo(name = "path") var path: String,
                            @ColumnInfo(name = "timeLimit") var timeLimit : Long?
){
    constructor():this(null,"","",0)
}