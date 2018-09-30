package com.example.company.myapplication.DBTables

import android.arch.persistence.room.*

@Entity(tableName = "TrainingsData")
data class TrainingsData(@PrimaryKey(autoGenerate = true) var id: Int?,
                         @ColumnInfo(name = "presentationID") var presentationID: Int?,
                         @ColumnInfo(name = "queueNumber") var queueNumber: Int,
                         @ColumnInfo(name = "generalDuration") var generalDuration : Int?
){
    constructor():this(null,0,0,null)
}