package ru.spb.speech

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import ru.spb.speech.DBTables.PresentationData
import ru.spb.speech.DBTables.TrainingData
import ru.spb.speech.DBTables.TrainingSlideData
import ru.spb.speech.DBTables.helpers.TrainingDBHelper
import ru.spb.speech.DBTables.helpers.TrainingSlideDBHelper
import kotlin.math.sqrt

class TrainingStatisticsData (myContext: Context, presentationData: PresentationData?, trainingData: TrainingData?) {

    private val context = myContext
    private val presData = presentationData
    private val trainData = trainingData

    private var trainingSlideDBHelper: TrainingSlideDBHelper? = TrainingSlideDBHelper(context)
    private var trainingDBHelper = TrainingDBHelper(context)

    private val trainingList = trainingDBHelper.getAllTrainingsForPresentation(presentationData!!)
    val trainingCount = trainingList?.size

    private val trainingSlidesList = trainingSlideDBHelper?.getAllSlidesForTraining(trainingData!!)

    //Название презентации:
    val presName = presentationData?.name

    //--------------------Текущая тренировка:---------------------//

    //Дата и время начала:
    val dateOfCurTraining: String
        get() {
            return if(trainData != null){
                (DateUtils.formatDateTime(
                        context, trainData.timeStampInSec!! * context.resources!!.getInteger(R.integer.milliseconds_in_second), DateUtils.FORMAT_SHOW_DATE) + " | " +
                        DateUtils.formatDateTime(
                                context, trainData.timeStampInSec!! * context.resources!!.getInteger(R.integer.milliseconds_in_second), DateUtils.FORMAT_SHOW_TIME))
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_cur_training_data))
                context.getString(R.string.error_accessing_the_cur_training_data)
            }
        }
    //Длительность тренировки:
    val currentTrainingTime: Long
        get() {
            var curTrTime: Long = 0
            if (trainingSlidesList != null) {
                for (slide in trainingSlidesList)
                    curTrTime += slide.spentTimeInSec!!
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_training_list))
                return -1
            }
            return curTrTime
        }
    //Проработано слайдов / всего слайдов:
    val curSlides: Int?
        get() {
            return if (trainData != null){
                trainingSlideDBHelper?.getAllSlidesForTraining(trainData)?.count()
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_cur_training_data))
                -1
            }
        }
    val slides = presentationData?.pageCount!!
    //Ограничение времени на доклад:
    val reportTimeLimit = presentationData?.timeLimit
    //Слов сказано:
    val curWordCount: Int
        get() {
            return if (trainData != null) {
                val tempWords = trainData.allRecognizedText.split(" ")
                var curWordCount = 0
                for (word in tempWords) {
                    if(word != ""){
                        curWordCount += 1
                    }
                }
                curWordCount
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_cur_training_data))
                -1
            }
        }
    //Оценка за тренировку:
    val trainingGrade: Int
        get() {
            return if(trainData != null){
                calcOfTheTrainingGrade(trainingSlideDBHelper?.getAllSlidesForTraining(trainData), presData?.timeLimit!!.toFloat()).toInt()
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_cur_training_data))
                -1
            }
        }

    //--------------------Статистика тренировок:---------------------//

    //Дата первой тренировки:
    val dateOfFirstTraining: String
        get() {
            return if(trainingList != null) {
                (DateUtils.formatDateTime(
                        context, trainingList[0].timeStampInSec!! * context.resources.getInteger(R.integer.milliseconds_in_second), DateUtils.FORMAT_SHOW_DATE) + " | " +
                        DateUtils.formatDateTime(
                                context, trainingList[0].timeStampInSec!! * context.resources.getInteger(R.integer.milliseconds_in_second), DateUtils.FORMAT_SHOW_TIME))
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_training_list))
                context.getString(R.string.error_accessing_the_training_list)
            }
        }
    //Тренировок полных / всего:
    val countOfCompleteTraining: Int
        get() {
            return if(trainingList != null){
                var countTr = 0
                for (training in trainingList){
                    val slides = trainingSlideDBHelper?.getAllSlidesForTraining(training)
                    if(slides?.count() == presData?.pageCount!!) {
                        countTr += 1
                    }
                }
                countTr
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_training_list))
                -1
            }
        }
    //Попали в регламент / всего:
    val fallIntoReg: Int
        get() {
            return if(trainingList != null){
                var fallIntoRegTemp = 0
                for (training in trainingList){
                    var timeOfCurrentTrain = 0L
                    val slides = trainingSlideDBHelper?.getAllSlidesForTraining(training)
                    if (slides != null) {
                        for (page in slides) {
                            timeOfCurrentTrain += page.spentTimeInSec!!
                        }
                        if(timeOfCurrentTrain < presData?.timeLimit!!){
                            fallIntoRegTemp += 1
                        }
                    } else {
                        Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_training_slides))
                        return -1
                    }
                }
                fallIntoRegTemp
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_training_list))
                -1
            }
        }
    //Среднее отклонение от ограничения:
    val averageExtraTime = getMaxAndMinAndAverageAndExtraTime(context.resources.getString(R.string.mean_deviation_from_the_limit))
    //Максимальное и минимальное время тренировки:
    val maxTrainTime = getMaxAndMinAndAverageAndExtraTime(context.resources.getString(R.string.max_training_time))
    val minTrainTime = getMaxAndMinAndAverageAndExtraTime(context.resources.getString(R.string.min_training_time))
    //Среднее время тренировки:
    val averageTime = getMaxAndMinAndAverageAndExtraTime(context.resources.getString(R.string.average_time))

    //Сказано слов всего:
    val allWords: Int
        get(){
            if(trainingList != null){
                var allWordsTemp: Int = context.resources.getInteger(R.integer.zero)
                for (training in trainingList){
                    val tempWords = training.allRecognizedText.split(" ")
                    val finalMassOfWords = ArrayList<String>()
                    for (i in tempWords){
                        if (i != ""){
                            finalMassOfWords.add(i)
                        }
                    }
                    allWordsTemp += finalMassOfWords.size
                }
                return allWordsTemp
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_training_list))
                return -1
            }
        }

    //Средняя оценка:
    val averageEarn: Float
        get(){
            return if(trainingList != null && trainingCount != null && presData != null) {
                var averageEarnTemp = context.resources.getDimension(R.dimen.zero_float)
                for (training in trainingList){
                    val score = calcOfTheTrainingGrade( trainingSlideDBHelper?.getAllSlidesForTraining(training), presData.timeLimit!!.toFloat())
                    averageEarnTemp += score
                }
                averageEarnTemp/trainingCount
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_the_training_list))
                -1f
            }
        }
    //Минимальная оценка:
    val minEarn: Float
        get(){
            return if(trainingList != null && trainingCount != null && presData != null) {
                var minEarnTemp = calcOfTheTrainingGrade( trainingSlideDBHelper?.getAllSlidesForTraining(trainingList[0]), presData.timeLimit!!.toFloat())
                for (training in trainingList){
                    val score = calcOfTheTrainingGrade( trainingSlideDBHelper?.getAllSlidesForTraining(training), presData.timeLimit!!.toFloat())
                    if (minEarnTemp > score){
                        minEarnTemp = score
                    }
                }
                minEarnTemp
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, "error in getting a list of trainings or presentation Data")
                -1f
            }
        }
    //Максимальная оценка:
    val maxEarn: Float
        get(){
            return if(trainingList != null && trainingCount != null && presData != null) {
                var maxEarnTemp = calcOfTheTrainingGrade( trainingSlideDBHelper?.getAllSlidesForTraining(trainingList[0]), presData.timeLimit!!.toFloat())
                for (training in trainingList){
                    val score = calcOfTheTrainingGrade( trainingSlideDBHelper?.getAllSlidesForTraining(training), presData.timeLimit!!.toFloat())
                    if (maxEarnTemp < score){
                        maxEarnTemp = score
                    }
                }
                maxEarnTemp
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.resources.getString(R.string.error_accessing_the_training_list_or_presentation_data))
                -1f
            }
        }


    private fun getMaxAndMinAndAverageAndExtraTime(minOrMax: String): Long {
        val (maxTrainTime, minTrainTime, averageExtraTime, averageTime) = calcMinAndMaxTime()
        return when (minOrMax) {
            context.resources.getString(R.string.max_training_time) -> maxTrainTime
            context.resources.getString(R.string.min_training_time) -> minTrainTime
            context.resources.getString(R.string.mean_deviation_from_the_limit) -> averageExtraTime
            context.resources.getString(R.string.average_time) -> averageTime
            else -> -1L
        }
    }

    data class TimeOfTraining(val maxTime: Long, val minTime: Long, val averageExtraTime: Long, val averageTime: Long)
    private fun calcMinAndMaxTime(): TimeOfTraining{
        if (trainingList != null && presData != null && trainingCount != null){
            var maxTime = 0L
            var minTime = 0L
            var countFlag = true
            var allAverageTime = 0L
            val averageExtraTime: Long
            var totalTime = 0L
            val averageTime: Long
            for (training in trainingList) {
                var timeOfCurrentTrain = 0L
                val slides = trainingSlideDBHelper?.getAllSlidesForTraining(training) ?: continue
                for (page in slides) {
                    timeOfCurrentTrain += page.spentTimeInSec!!
                    totalTime += page.spentTimeInSec!!
                }
                if (timeOfCurrentTrain > maxTime) maxTime = timeOfCurrentTrain
                if (timeOfCurrentTrain < minTime) minTime = timeOfCurrentTrain
                if (countFlag) {
                    minTime = timeOfCurrentTrain
                    countFlag = false
                }
                if (timeOfCurrentTrain >= presData.timeLimit!!) {
                    allAverageTime += timeOfCurrentTrain - presData.timeLimit!!
                }
            }
            averageExtraTime = if(trainingCount - fallIntoReg > 0) {
                allAverageTime / (trainingCount - fallIntoReg)
            } else {
                0L
            }
            averageTime = totalTime / trainingCount
            return TimeOfTraining(maxTime, minTime, averageExtraTime, averageTime)
        } else {
            Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.resources.getString(R.string.error_accessing_the_training_list_or_presentation_data))
            return TimeOfTraining(0L, 0L, 0L, 0L)
        }
    }

    private fun calcOfTheTrainingGrade(slideInTraining: MutableList<TrainingSlideData>?, x0ReportTimeLimit: Float) : Float{

        val dxDiffBtwTrainTimeAndLim: Float = if (currentTrainingTime > x0ReportTimeLimit*3){
            x0ReportTimeLimit
        } else Math.abs(currentTrainingTime - x0ReportTimeLimit)

        var dySpeechVelDispersion = context.resources.getDimension(R.dimen.zero_float)

        val speedList = ArrayList<Float>()
        val timeList = ArrayList<Long>()
        var averSpeed = context.resources.getDimension(R.dimen.zero_float)
        var curAverTime = context.resources.getDimension(R.dimen.zero_float)
        if (slideInTraining != null) {
            for (slide in slideInTraining) {
                if (slide.knownWords != "") speedList.add(slide.knownWords!!.split(" ").size.toFloat() / slide.spentTimeInSec!!.toFloat() * 60.0f)
                timeList.add(slide.spentTimeInSec!!)
            }
        } else {
            Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, context.getString(R.string.error_accessing_training_slides))
            return -1f
        }
        averSpeed = speedList.sum()
        for (i in timeList){
            curAverTime += i
        }
        averSpeed/=speedList.size
        curAverTime/=timeList.size
        for (i in speedList){
            dySpeechVelDispersion += Math.pow((i - averSpeed).toDouble(), context.resources.getDimension(R.dimen.quadrant_degree_float).toDouble()).toFloat()
        }

        dySpeechVelDispersion /= speedList.size

        var dzTimeDispersionOnSlides = context.resources.getDimension(R.dimen.zero_float)

        for (i in timeList){
            dzTimeDispersionOnSlides += Math.pow((i - curAverTime).toDouble(), context.resources.getDimension(R.dimen.quadrant_degree_float).toDouble()).toFloat()
        }

        dzTimeDispersionOnSlides /= timeList.size
        val xExerciseTimeFactor  = context.resources.getDimension(R.dimen.unit_float) - (dxDiffBtwTrainTimeAndLim/x0ReportTimeLimit)
        val ySpeechSpeedFactor = context.resources.getDimension(R.dimen.unit_float)/(sqrt(dySpeechVelDispersion) + context.resources.getDimension(R.dimen.unit_float))
        val zTimeOnSlidesFactor = context.resources.getDimension(R.dimen.unit_float)/(sqrt(dzTimeDispersionOnSlides) + context.resources.getDimension(R.dimen.unit_float))

        return (context.resources.getInteger(R.integer.transfer_to_interest)*(xExerciseTimeFactor+ySpeechSpeedFactor+zTimeOnSlidesFactor)/context.resources.getDimension(R.dimen.number_of_factors))
    }

}