package ru.spb.speech.firebase.model

class FullTrainingStatistic(
        val trainingID: Int,
        var presentationName: String,
        var lastTrainingDate: String,
        var firstTrainingDate: String,
        var finishedTrainings_allTrainings: String,
        var copedCount_allCount: String,
        var averageDeviationLimitRestriction: String,
        var maxTrainingTime: String,
        var minTrainingTime: String,
        var averageTrainingTime: String,
        var countOfAllWords: String,
        var average_min_maxMarks: String
) {
    constructor(trainingID: Int): this(trainingID, "", "", "",
            "", "", "", "",
            "", "", "", "")
}