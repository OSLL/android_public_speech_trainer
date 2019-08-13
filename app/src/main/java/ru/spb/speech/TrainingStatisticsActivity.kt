package ru.spb.speech

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.design.widget.BottomSheetDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import ru.spb.speech.database.helpers.TrainingSlideDBHelper
import ru.spb.speech.TrainingHistoryActivity.Companion.launchedFromHistoryActivityFlag
import ru.spb.speech.appSupport.PdfToBitmap
import ru.spb.speech.appSupport.ProgressHelper
import ru.spb.speech.vocabulary.TextHelper
import ru.spb.speech.database.interfaces.PresentationDataDao
import ru.spb.speech.database.PresentationData
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.TrainingData
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IValueFormatter
import kotlinx.android.synthetic.main.activity_training_statistics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.spb.speech.appSupport.showStatisticsFragments
import ru.spb.speech.fragments.statistic_fragments.AudioStatisticsFragment
import ru.spb.speech.fragments.statistic_fragments.SpeedStatisticsFragment
import ru.spb.speech.appSupport.TrainingStatisticsData
import ru.spb.speech.fragments.statistic_fragments.TimeOnEachSlideFragment
import java.io.*
import java.text.BreakIterator
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.abs

var url = ""
var speed_statistics: Int? = null

const val ACTIVITY_TRAINING_STATISTIC_NAME = ".TrainingStatisticActivity"

private const val REFERENCE_WORD_FREQUENCY = 72.9
private const val FREQUENCY_MARGINAL_DEVIATION = 28.6

@Suppress("DEPRECATION")
class TrainingStatisticsActivity : AppCompatActivity() {

    private lateinit var presentationDataDao: PresentationDataDao
    private var presentationData: PresentationData? = null
    private lateinit var trainingSlideDBHelper: TrainingSlideDBHelper

    private var trainingData: TrainingData? = null

    private var finishBmp: Bitmap? = null
    private var pdfReader: PdfToBitmap? = null

    private var bmpBase: Bitmap? = null

    private var currentTrainingTime: Long = 0

    private var wordCount: Int = 0
    private val activityRequestCode = 101

    private var averageTimePerSlide: Double = 0.0
    private var perfectAverageTimePerSlide: Double = 58.8
    private var middleTimeError: Double = 30.8

    private var recommendationString: String = ""
    private val speechDataBase by lazy { SpeechDataBase.getInstance(this)!! }


    private lateinit var progressHelper: ProgressHelper

    var trainingStatisticsData: TrainingStatisticsData? = null

    @SuppressLint("LongLogTag", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_statistics)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressHelper = ProgressHelper(this, root_view_training_statistics, listOf(share1, returnTraining))

        presentationDataDao = SpeechDataBase.getInstance(this)!!.PresentationDataDao()
        val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID),-1)
        val trainingId = intent.getIntExtra(getString(R.string.CURRENT_TRAINING_ID),-1)
        if (presId > 0 && trainingId > 0) {
            presentationData = presentationDataDao.getPresentationWithId(presId)
            trainingData = SpeechDataBase.getInstance(this)?.TrainingDataDao()?.getTrainingWithId(trainingId)
        }
        else {
            Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, "stat_act: wrong ID")
            return
        }

        showStatisticsFragments(
                AudioStatisticsFragment() to R.id.audio_analyzer_statistics_container,
                TimeOnEachSlideFragment() to R.id.time_on_each_slide_chart_box_activity_training_statistics,
                SpeedStatisticsFragment() to R.id.speed_line_chart_container,
                trainingId = trainingId)

        if (intent.getIntExtra(getString(R.string.launchedFromHistoryActivityFlag),-1) == launchedFromHistoryActivityFlag) returnTraining.visibility = View.GONE

        trainingSlideDBHelper = TrainingSlideDBHelper(this)

        pdfReader = PdfToBitmap(presentationData!!, this)

        val trainingSlidesList = trainingSlideDBHelper.getAllSlidesForTraining(trainingData!!) ?: return

        for (slide in trainingSlidesList)
            currentTrainingTime += slide.spentTimeInSec!!

        trainingStatisticsData = TrainingStatisticsData(this, presentationData, trainingData)

        for (slide in trainingSlidesList){
            currentTrainingTime += slide.spentTimeInSec!!
        }

        var indexOfSlide: Int = 0
        val slidesWithUpError = arrayListOf<Int>()
        val slidesWithLowError = arrayListOf<Int>()

        for (slide in trainingSlidesList){
            currentTrainingTime += slide.spentTimeInSec!!
        }

        averageTimePerSlide = currentTrainingTime.toDouble()/trainingSlidesList.size
        var numbersOfSlidesWithError = ""

        if (abs(averageTimePerSlide - perfectAverageTimePerSlide) > middleTimeError){
            if (averageTimePerSlide > perfectAverageTimePerSlide){
                recommendationString += getString(R.string.recommendation_speed_with_error, "понизить")
            }
            else{
                recommendationString += getString(R.string.recommendation_speed_with_error, "повысить")
            }
        }
        else {
            for (slide in trainingSlidesList) {
                currentTrainingTime += slide.spentTimeInSec!!
                indexOfSlide++
                if (abs((slide.spentTimeInSec!! - averageTimePerSlide)) > middleTimeError)
                    if (slide.spentTimeInSec!! - averageTimePerSlide > 0){
                        slidesWithUpError.add(indexOfSlide)
                    }
                    else slidesWithLowError.add(indexOfSlide)
            }

            if (slidesWithUpError.isNotEmpty()) {

                recommendationString = getString(R.string.recommendation_speed_without_error, slidesWithUpError.joinToString(separator = ", ", postfix = " "))
                recommendationString += getString(R.string.recommendation_speed_decrease_info)
            }

            if (slidesWithLowError.isNotEmpty()) {
                recommendationString += getString(R.string.recommendation_speed_without_error, slidesWithLowError.joinToString(separator = ", ", postfix = " "))
                recommendationString += getString(R.string.recommendation_speed_increase_info)
            }
        }

        val drawer = Thread(Runnable {
            drawPict()
        })
        drawer.start()

        var frequencyRecommendationMessage = ""

        GlobalScope.launch(Dispatchers.IO) {
            frequencyRecommendationMessage = sendFrequencyWordRecommendation()
        }

        improve_mark_button.setOnClickListener {
            val intent = Intent(this, RecommendationActivity::class.java)
            intent.putExtra(getString(R.string.recommendation_key), recommendationString)
            intent.putExtra(getString(R.string.frequency_recommendation_key), frequencyRecommendationMessage)

            startActivity(intent)
        }

        question.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val bottomSheet = layoutInflater.inflate(R.layout.evaluation_information_sheet, null)

            //bottomSheet.closeTheQuestion.setOnClickListener { dialog.dismiss() }

            dialog.setContentView(bottomSheet)
            dialog.show()

        }

        share1.setOnClickListener {
            try {
                drawer.join()
                url = MediaStore.Images.Media.insertImage(this.contentResolver, finishBmp, "title", null)
            }catch (e: Exception) {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, e.toString())
            }
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.putExtra(Intent.EXTRA_STREAM,  Uri.parse(url))
            sharingIntent.type = "image/jpg"
            startActivityForResult(Intent.createChooser(sharingIntent, "Share with friends"), activityRequestCode)
        }

        returnTraining.setOnClickListener {
            val i = Intent(this, TrainingActivity::class.java)
            i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), presentationData?.id)
            startActivity(i)
            finish()
        }

        export.setOnClickListener {
            val trainingsFile: File?
            val sdState = android.os.Environment.getExternalStorageState()
            trainingsFile = if (sdState == android.os.Environment.MEDIA_MOUNTED) {
                val sdDir = android.os.Environment.getExternalStorageDirectory()
                File(sdDir, getString(R.string.training_statistics_directory))
            } else {
                this.cacheDir
            }
            if (!trainingsFile!!.exists())
                trainingsFile.mkdir()

            val curTrainingFile: File?
            curTrainingFile = if (sdState == android.os.Environment.MEDIA_MOUNTED) {
                val sdDir = android.os.Environment.getExternalStorageDirectory()
                File(sdDir, "${getString(R.string.training_statistics_directory)}/${trainingStatisticsData?.presName}")
            } else {
                this.cacheDir
            }
            if (!curTrainingFile!!.exists())
                curTrainingFile.mkdir()

            try {
                val textFile = File(Environment.getExternalStorageDirectory(), "${getString(R.string.training_statistics_directory)}/${trainingStatisticsData?.presName}/${trainingStatisticsData?.dateOfCurTraining}.txt")
                val fos = FileOutputStream(textFile)
                fos.write(trainStatInTxtFormat().toByteArray())
                fos.close()
                Toast.makeText(this, "${getString(R.string.successful_export_statistics)} ${getString(R.string.training_statistics_directory)}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.d(ACTIVITY_TRAINING_STATISTIC_NAME, getString(R.string.error_creating_text_file))
                Toast.makeText(this, getString(R.string.error_export_statistics), Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        val trainingSpeedData = HashMap<Int, Float>()
        val trainingSlideList = trainingSlideDBHelper.getAllSlidesForTraining(trainingData!!)

        val presentationSpeedData = mutableListOf<BarEntry>()
        for ((i, slide) in trainingSlideList!!.withIndex()) {
            var speed = 0f
            if (slide.knownWords != "") speed = slide.knownWords!!.split(" ").size.toFloat() / slide.spentTimeInSec!!.toFloat() * resources.getDimension(R.dimen.number_of_seconds_in_a_minute_float)
            trainingSpeedData[i] = speed
            presentationSpeedData.add(BarEntry((i).toFloat(), speed))
        }

        val presentationTop10Words = TextHelper(this.resources.getStringArray(R.array.prepositionsAndConjunctions))
                .getTop10WordsRmConjStemm(trainingData!!.allRecognizedText)

        val entries = ArrayList<PieEntry>()
        for (pair in presentationTop10Words){
            entries.add(PieEntry(pair.second.toFloat(), pair.first))
        }

        printPiechart(entries)

        val averageSpeed = getAverageSpeed(trainingSpeedData)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val optimalSpeed = sharedPreferences.getString(getString(R.string.speed_key), "120")
        val isExportVisible = sharedPreferences.getBoolean("deb_statistics_export", false)
        if (!isExportVisible) {
            export.visibility = View.VISIBLE
        }

        val bestSlide = getBestSlide(trainingSpeedData, optimalSpeed.toInt())
        val worstSlide = getWorstSlide(trainingSpeedData, optimalSpeed.toInt())

        if (trainingStatisticsData?.curWordCount == 0){
            earnOfTrain.text = "${getString(R.string.earnings_of_training)} 0.0 ${getString(R.string.maximum_mark_for_training)}"
        }
        else{
            earnOfTrain.text = "${getString(R.string.earnings_of_training)} ${trainingStatisticsData?.trainingGrade?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} ${getString(R.string.maximum_mark_for_training)}"
        }

        x_exercise_time_factor.append(" ${((trainingStatisticsData?.xExerciseTimeFactor)!! * resources.getInteger(R.integer.transfer_to_interest)/resources.getDimension(R.dimen.number_of_factors)).format(1)}")
        y_speech_speed_factor.append(" ${((trainingStatisticsData?.ySpeechSpeedFactor)!! * resources.getInteger(R.integer.transfer_to_interest)/resources.getDimension(R.dimen.number_of_factors)).format(1)}")
        z_time_on_slides_factor.append(" ${((trainingStatisticsData?.zTimeOnSlidesFactor)!! * resources.getInteger(R.integer.transfer_to_interest)/resources.getDimension(R.dimen.number_of_factors)).format(1)}")

        var countOfParasites = ((trainingStatisticsData!!.countOfParasites.toFloat() / trainingStatisticsData!!.curWordCount.toFloat())*resources.getInteger(R.integer.transfer_to_interest)).format(0)
        if(trainingStatisticsData!!.countOfParasites == 0L){
            countOfParasites = "0.0"
        }

        textView.text = getString(R.string.average_speed) +
                " %.2f ${getString(R.string.speech_speed_units)}\n".format(averageSpeed) +
                getString(R.string.best_slide) + " $bestSlide\n" +
                getString(R.string.worst_slide) + " $worstSlide\n" +
                getString(R.string.training_time) + " ${getStringPresentationTimeLimit(trainingStatisticsData?.currentTrainingTime)}\n" +
                getString(R.string.count_of_slides) + " ${trainingSlidesList.size}/${presentationData?.pageCount!!}\n" +
                getString(R.string.word_share_of_parasites) + " $countOfParasites " + getString(R.string.percent)

        speed_statistics = trainingStatisticsData?.curWordCount
        sharedPreferences.edit().putInt(getString(R.string.num_of_words_spoken), trainingStatisticsData!!.curWordCount).putInt(getString(R.string.total_words_count), trainingStatisticsData!!.allWords).apply()
    }

    private fun sendFrequencyWordRecommendation(): String{
        val arrayOfFrequency = trainingStatisticsData!!.wordFrequencyPerSlide
        var averageFrequency = 0f
        for (freq in arrayOfFrequency) {
            averageFrequency += freq
        }
        averageFrequency /= trainingStatisticsData!!.slides
        if(averageFrequency < REFERENCE_WORD_FREQUENCY - FREQUENCY_MARGINAL_DEVIATION) {
            return getString(R.string.increase_the_pace_of_speech_recommendation)
        } else if (averageFrequency > REFERENCE_WORD_FREQUENCY + FREQUENCY_MARGINAL_DEVIATION) {
            return getString(R.string.lower_the_pace_of_speech_recommendation)
        } else {
            val aboveAverage = mutableListOf<Int>()
            val belowAverage = mutableListOf<Int>()
            for (freq in 0 until arrayOfFrequency.count()){
                if(freq < REFERENCE_WORD_FREQUENCY - FREQUENCY_MARGINAL_DEVIATION){
                    belowAverage.add(freq+1)
                }
                if(freq > REFERENCE_WORD_FREQUENCY + FREQUENCY_MARGINAL_DEVIATION) {
                    aboveAverage.add(freq+1)
                }
            }
            if (aboveAverage.count() == 0 && belowAverage.count() == 0) {
                return getString(R.string.no_frequency_recommendation)
            } else {
                var frequencyRecommendationMessage = "${getString(R.string.frequency_title)} "
                if(aboveAverage.count() != 0) {
                    for (freq in 0 until aboveAverage.count()) {
                        frequencyRecommendationMessage += "${aboveAverage[freq]}"
                        if (freq < aboveAverage.count()-1){
                            frequencyRecommendationMessage += ", "
                        }
                    }
                    frequencyRecommendationMessage += " ${getString(R.string.increase_frequency_title)}"
                }
                if(belowAverage.count() != 0) {
                    if(aboveAverage.count() != 0){
                        frequencyRecommendationMessage += "${getString(R.string.frequency_title_two)} "
                    }
                    for (freq in 0 until belowAverage.count()) {
                        frequencyRecommendationMessage += "${belowAverage[freq]}"
                        if (freq < belowAverage.count()-1){
                            frequencyRecommendationMessage += ", "
                        }
                    }
                    frequencyRecommendationMessage += " ${getString(R.string.lover_frequency_title)}"
                }
                return frequencyRecommendationMessage
            }
        }
    }

    fun Float.format(digits: Int) = java.lang.String.format("%.${digits}f", this)!!

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            super.onBackPressed()
            return true
        }
        return false
    }

    override fun onPause() {
        progressHelper.show()
        super.onPause()
    }

    override fun onResume() {
        progressHelper.hide()
        super.onResume()
    }

    private fun drawPict() {

        pdfReader?.getBitmapForSlide(resources.getInteger(R.integer.zero))
        bmpBase = pdfReader?.saveSlideImage("tempImage.pdf")

        Log.d(ACTIVITY_TRAINING_STATISTIC_NAME, "training count: ${trainingStatisticsData?.trainingCount}")

        val width = bmpBase?.width
        val height = bmpBase?.height

        val presName = trainingStatisticsData?.presName

        if(width != null && height != null) {
            val nWidth: Int = width
            val nHeight: Int = height
            finishBmp = Bitmap.createBitmap(nWidth, nHeight + resources.getInteger(R.integer.block_height_with_last_workout) + resources.getInteger(R.integer.block_height_with_training_statistics), Bitmap.Config.ARGB_8888)

            val whitePaint = Paint()
            whitePaint.style = Paint.Style.FILL
            whitePaint.color = Color.WHITE

            val nameBmp = Bitmap.createBitmap(nWidth, resources.getInteger(R.integer.height_of_block_with_name), Bitmap.Config.ARGB_8888)
            val nameC = Canvas(nameBmp)
            nameC.drawPaint(whitePaint)
            val namePaint = Paint()
            namePaint.color = Color.BLACK
            namePaint.style = Paint.Style.FILL
            namePaint.isAntiAlias = true
            if(presName?.length != null) {
                when {
                    presName.length < resources.getInteger(R.integer.length_of_the_presentation_title_32) -> namePaint.textSize = resources.getDimension(R.dimen.font_size_24)
                    presName.length < resources.getInteger(R.integer.length_of_the_presentation_title_37) -> namePaint.textSize = resources.getDimension(R.dimen.font_size_20)
                    else -> namePaint.textSize = resources.getDimension(R.dimen.font_size_16)
                }
                namePaint.isUnderlineText = true
                if (presName.length < resources.getInteger(R.integer.length_of_the_presentation_title_30)) {
                    nameC.drawText(presName, ((resources.getInteger(R.integer.length_of_the_presentation_title_32) - presName.length).toFloat()) * resources.getDimension(R.dimen.x_indent_multiplier_6_5), resources.getDimension(R.dimen.y_indent_multiplier_30), namePaint)
                } else
                    nameC.drawText(presName, resources.getDimension(R.dimen.x_indent_multiplier_20), resources.getDimension(R.dimen.y_indent_multiplier_30), namePaint)
            }

            val lastTrainingBmp = Bitmap.createBitmap(nWidth, resources.getInteger(R.integer.block_height_with_last_workout), Bitmap.Config.ARGB_8888)
            val ltC = Canvas(lastTrainingBmp)
            ltC.drawPaint(whitePaint)
            val ltP = Paint()
            ltP.color = Color.BLACK
            ltP.style = Paint.Style.FILL
            ltP.isAntiAlias = true
            ltP.textSize = resources.getDimension(R.dimen.font_size_20)
            ltP.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            ltC.drawText(getString(R.string.cur_training_title), resources.getDimension(R.dimen.x_indent_multiplier_20), resources.getDimension(R.dimen.y_indent_multiplier_20), ltP)
            ltP.textSize = resources.getDimension(R.dimen.font_size_17)
            ltP.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

            ltC.drawText(getString(R.string.date_and_time_to_start_training) + " " + trainingStatisticsData?.dateOfCurTraining, resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_43), ltP)
            ltC.drawText(getString(R.string.time_of_training) + getStringPresentationTimeLimit(trainingStatisticsData?.currentTrainingTime!!), resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_66), ltP)

            ltC.drawText(getString(R.string.worked_out_a_slide) + " " + trainingStatisticsData?.curSlides + " / " + trainingStatisticsData?.slides, resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_89), ltP)
            ltC.drawText(getString(R.string.time_limit_training) + " " + getStringPresentationTimeLimit(trainingStatisticsData?.reportTimeLimit), resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_112), ltP)
            ltC.drawText(getString(R.string.num_of_words_spoken) + " " + trainingStatisticsData?.curWordCount, resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_135), ltP)
            ltC.drawText("${getString(R.string.earnings_of_training)} ${trainingStatisticsData?.trainingGrade?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} ${getString(R.string.maximum_mark_for_training)}", resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_158), ltP)

            val trainingStatisticsBmp = Bitmap.createBitmap(nWidth, resources.getInteger(R.integer.block_height_with_training_statistics), Bitmap.Config.ARGB_8888)
            val tsC = Canvas(trainingStatisticsBmp)
            tsC.drawPaint(whitePaint)
            val tsP = Paint()
            tsP.color = Color.BLACK
            tsP.style = Paint.Style.FILL
            tsP.isAntiAlias = true
            tsP.textSize = resources.getDimension(R.dimen.font_size_20)
            tsP.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            tsC.drawText(getString(R.string.training_statistic_title), resources.getDimension(R.dimen.x_indent_multiplier_20), resources.getDimension(R.dimen.y_indent_multiplier_25), tsP)
            tsP.textSize = resources.getDimension(R.dimen.font_size_17)
            tsP.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

            tsC.drawText(getString(R.string.date_of_first_training) + " " + trainingStatisticsData?.dateOfFirstTraining, resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_48), tsP)
            tsC.drawText(getString(R.string.training_completeness) + " " + trainingStatisticsData?.countOfCompleteTraining + " / " + trainingStatisticsData?.trainingCount, resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_71), tsP)
            tsC.drawText(getString(R.string.getting_into_the_regulations) + " " + trainingStatisticsData?.fallIntoReg + " / " + trainingStatisticsData?.trainingCount , resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_94), tsP)

            tsC.drawText(getString(R.string.mean_deviation_from_the_limit) + " " + getStringPresentationTimeLimit(trainingStatisticsData?.averageExtraTime) , resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_117), tsP)
            tsC.drawText(getString(R.string.max_training_time) + getStringPresentationTimeLimit(trainingStatisticsData?.maxTrainTime), resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_140), tsP)
            tsC.drawText(getString(R.string.min_training_time) + getStringPresentationTimeLimit(trainingStatisticsData?.minTrainTime), resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_163), tsP)
            tsC.drawText(getString(R.string.average_time) + getStringPresentationTimeLimit(trainingStatisticsData?.averageTime), resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_186), tsP)
            tsC.drawText(getString(R.string.total_words_count) + " " + trainingStatisticsData?.allWords, resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_209), tsP)

            var countOfParasites = ((trainingStatisticsData!!.countOfParasites.toFloat() / trainingStatisticsData!!.curWordCount.toFloat())*resources.getInteger(R.integer.transfer_to_interest)).format(0)
            if(trainingStatisticsData!!.countOfParasites == 0L){
                countOfParasites = "0.0"
            }

            tsC.drawText(getString(R.string.word_share_of_parasites) + " $countOfParasites " + getString(R.string.percent), resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_232), tsP)
            tsC.drawText(getString(R.string.average_earning_1), resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_255), tsP)
            tsC.drawText(getString(R.string.average_earning_2) + " " +
                    trainingStatisticsData?.averageEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score)) + " / " +
                    trainingStatisticsData?.minEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score)) + " / " +
                    trainingStatisticsData?.maxEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score)), resources.getDimension(R.dimen.x_indent_multiplier_60),
                    resources.getDimension(R.dimen.y_indent_multiplier_278), tsP)

            val canvas = Canvas(finishBmp)
            val paint = Paint()
            canvas.drawBitmap(bmpBase, resources.getDimension(R.dimen.left_indent_multiplier_0), resources.getDimension(R.dimen.top_indent_multiplier_0), paint)
            canvas.drawBitmap(nameBmp, resources.getDimension(R.dimen.left_indent_multiplier_0), nHeight.toFloat(), paint)
            canvas.drawBitmap(lastTrainingBmp, resources.getDimension(R.dimen.left_indent_multiplier_0), nHeight.toFloat() + resources.getDimension(R.dimen.top_indent_multiplier_40), paint)
            canvas.drawBitmap(trainingStatisticsBmp, resources.getDimension(R.dimen.left_indent_multiplier_0), nHeight.toFloat() + resources.getDimension(R.dimen.top_indent_multiplier_200), paint)
        }
    }

    private fun trainStatInTxtFormat():String {
        return "${getString(R.string.name_of_pres)} ${trainingStatisticsData?.presName}\n\n" +
                "\t${getString(R.string.cur_training_title)}\n" +
                "${getString(R.string.date_and_time_to_start_training)} ${trainingStatisticsData?.dateOfCurTraining}\n" +
                "${getString(R.string.worked_out_a_slide)} ${trainingStatisticsData?.curSlides} / ${trainingStatisticsData?.slides}\n" +
                "${getString(R.string.time_limit_training)} ${getStringPresentationTimeLimit(trainingStatisticsData?.reportTimeLimit)}\n" +
                "${getString(R.string.num_of_words_spoken)} ${trainingStatisticsData?.curWordCount}\n" +
                "${getString(R.string.training_duration)} ${getStringPresentationTimeLimit(trainingStatisticsData?.currentTrainingTime)}\n" +
                "${getString(R.string.earnings_of_training)} ${trainingStatisticsData?.trainingGrade?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} ${getString(R.string.maximum_mark_for_training)}\n\n" +
                "\t${getString(R.string.training_statistic_title)}\n" +
                "${getString(R.string.date_of_first_training)} ${trainingStatisticsData?.dateOfFirstTraining}\n" +
                "${getString(R.string.training_completeness)} ${trainingStatisticsData?.countOfCompleteTraining} / ${trainingStatisticsData?.trainingCount}\n" +
                "${getString(R.string.getting_into_the_regulations)} ${trainingStatisticsData?.fallIntoReg} / ${trainingStatisticsData?.trainingCount}\n" +
                "${getString(R.string.mean_deviation_from_the_limit)} ${getStringPresentationTimeLimit(trainingStatisticsData?.averageExtraTime)}\n" +
                "${getString(R.string.max_training_time)} ${getStringPresentationTimeLimit(trainingStatisticsData?.maxTrainTime)}\n" +
                "${getString(R.string.min_training_time)} ${getStringPresentationTimeLimit(trainingStatisticsData?.minTrainTime)}\n" +
                "${getString(R.string.average_time)} ${getStringPresentationTimeLimit(trainingStatisticsData?.averageTime)}\n" +
                "${getString(R.string.total_words_count)} ${trainingStatisticsData?.allWords}\n" +
                "${getString(R.string.average_earning_1)}\n ${getString(R.string.average_earning_2)} ${trainingStatisticsData?.averageEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} / ${trainingStatisticsData?.minEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} / ${trainingStatisticsData?.maxEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))}"

    }

    private fun getCase(n: Int? , case1: String, case2: String, case3: String): String {
        if (n == null || n <= 0) {
            return "undefined"
        }

        val titles = arrayOf("$n $case1","$n $case2","$n $case3")
        val cases = arrayOf(2, 0, 1, 1, 1, 2)

        return " " + titles[if (n % 100 in 5..19) 2 else cases[if (n % 10 < 5) n % 10 else 5]]
    }

    @SuppressLint("UseSparseArrays")
    private fun getStringPresentationTimeLimit(t: Long?): String {

        if (t == null)
            return "undefined"

        var millisUntilFinishedVar: Long = t


        val minutes = TimeUnit.SECONDS.toMinutes(millisUntilFinishedVar)
        millisUntilFinishedVar -= TimeUnit.MINUTES.toSeconds(minutes)

        val seconds = millisUntilFinishedVar

        return String.format(
                Locale.getDefault(),
                " %02d:%02d",
                minutes, seconds
        )

    }

    private fun printPiechart (lineEntries: List<PieEntry>){

        val pieDataSet = PieDataSet(lineEntries, null)
        pieDataSet.valueFormatter = IValueFormatter { value, _, _, _ -> "${value.toInt()}" }
        val arrOfColors = intArrayOf(Color.RED, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN, Color.MAGENTA, Color.DKGRAY, Color.LTGRAY, Color.YELLOW, Color.BLACK)
        pieDataSet.setColors(arrOfColors,255)


        val data = PieData(pieDataSet)
        pie_chart.data = data

        pie_chart.centerText = getString(R.string.pie_chart_tittle)
        pie_chart.data.setValueTextSize(10f)
        pie_chart.data.setValueTextColor(Color.WHITE)
        pie_chart.setDrawSliceText(false)

        pie_chart.description.isEnabled = false

        pie_chart.animateY(1200)

        pie_chart.legend.position = Legend.LegendPosition.RIGHT_OF_CHART_CENTER

        pie_chart.invalidate()
    }

    private fun getTop10Words(text: String) : List<Pair<String, Int>> {
        val dictionary = HashMap<String, Int>()

        val iterator = BreakIterator.getWordInstance()
        iterator.setText(text)

        var endIndex = iterator.first()
        while (BreakIterator.DONE != endIndex) {
            val startIndex = endIndex
            endIndex = iterator.next()
            if (endIndex != BreakIterator.DONE && Character.isLetterOrDigit(text[startIndex])) {
                val word = text.substring(startIndex, endIndex)
                val count = dictionary[word] ?: 0
                dictionary[word] = count + 1
                wordCount++
            }
        }

        val result = ArrayList<Pair<String, Int>>()
        dictionary.onEach {
            val position = getPosition(result, it.value)
            if (position < 10)
                result.add(position, it.toPair())
            if (result.size > 10)
                result.removeAt(10)
        }
        return result
    }

    private fun getPosition(list : List<Pair<String, Int>>, value : Int) : Int {
        if (list.isEmpty())
            return 0
        for (i in list.indices) {
            if (value > list[i].second)
                return i
        }
        return list.size
    }
}