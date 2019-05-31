package ru.spb.speech.database

import android.arch.persistence.room.*

@Entity(tableName = "PresentationData")
data class PresentationData(@PrimaryKey(autoGenerate = true) var id: Int?,
                            @ColumnInfo(name = "name") var name: String,
                            @ColumnInfo(name = "stringUri") var stringUri: String,
                            @ColumnInfo(name = "timeLimit") var timeLimit : Long?,
                            @ColumnInfo(name = "pageCount") var pageCount: Int?,
                            @ColumnInfo(name = "presentationDate") var presentationDate: String,
                            @ColumnInfo(name = "notifications") var notifications: Boolean,
                            @ColumnInfo(name = "debugPresentationFlag") var debugFlag: Int,
                            @ColumnInfo(name = "trainingDataId") var trainingDataId: Int?,
                            @ColumnInfo(name = "imageBLOB", typeAffinity = ColumnInfo.BLOB) var imageBLOB: ByteArray?

){
    constructor():this(null,"","",null,0, "2019-1-1", false, 0, null, null)

    fun isUnfinished() : Boolean {
        return (this.name.isNullOrEmpty() || this.pageCount == 0 || imageBLOB == null)
    }
}