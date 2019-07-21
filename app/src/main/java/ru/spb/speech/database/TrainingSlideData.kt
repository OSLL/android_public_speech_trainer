package ru.spb.speech.database

import android.arch.persistence.room.*
import ru.spb.speech.appSupport.SlideInfo

@Entity(tableName = "TrainingSlideData")
data class TrainingSlideData(@PrimaryKey(autoGenerate = true) var id:Int?,
                             @ColumnInfo(name = "spentTimeInSec") var spentTimeInSec: Long?,
                             @ColumnInfo(name = "knownWords") var knownWords: String?,
                             @ColumnInfo(name = "nextSlideId") var nextSlideId : Int?,
                             @ColumnInfo(name = "silencePercentage") var silencePercentage: Double?,
                             @ColumnInfo(name = "pauseAverageLength") var pauseAverageLength: Long?,
                             @ColumnInfo(name = "longPausesAmount") var longPausesAmount: Int?
){
    constructor():this(null,0,null,null, null, null, null)
}

fun TrainingSlideData.updateAudioStatistics(slideInfo: SlideInfo) {
    this.silencePercentage = slideInfo.silencePercentage
    this.pauseAverageLength = slideInfo.pauseAverageLength
    this.longPausesAmount = slideInfo.longPausesAmount
}

fun List<TrainingSlideData>.toSlideInfoList(): List<SlideInfo> {
    val list = ArrayList<SlideInfo>()

    for ((index, slide) in this.withIndex())
        with (slide) {
            list.add(SlideInfo(index + 1,
                    silencePercentage?:0.0,
                    pauseAverageLength?:0,
                    longPausesAmount?:0
                    ))
        }

    return list
}

fun List<TrainingSlideData>.getTrainingLenInSec(): Long {
    var len = 0L
    this.forEach { len += it.spentTimeInSec ?: 0L }
    return len
}