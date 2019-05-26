package ru.spb.speech

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import ru.spb.speech.database.helpers.TrainingDBHelper
import ru.spb.speech.database.helpers.TrainingSlideDBHelper
import ru.spb.speech.TrainingHistoryActivity.Companion.launchedFromHistoryActivityFlag
import ru.spb.speech.appSupport.PdfToBitmap
import ru.spb.speech.appSupport.ProgressHelper
import ru.spb.speech.vocabulary.PrepositionsAndConjunctions
import ru.spb.speech.fragments.TimeOnEachSlideChartFragment
import ru.spb.speech.database.interfaces.PresentationDataDao
import ru.spb.speech.database.PresentationData
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.TrainingData
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_training_statistics.*
import java.io.*
import java.text.BreakIterator
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.round

var url = ""
var speed_statistics: Int? = null

const val ACTIVITY_TRAINING_STATISTIC_NAME = ".TrainingStatisticActivity"

@Suppress("DEPRECATION")
class TrainingStatisticsActivity : AppCompatActivity() {

    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null
    private var trainingSlideDBHelper: TrainingSlideDBHelper? = null
    private var trainingDBHelper: TrainingDBHelper? = null

    private var trainingData: TrainingData? = null

    private var finishBmp: Bitmap? = null
    private var pdfReader: PdfToBitmap? = null

    private var bmpBase: Bitmap? = null

    private var currentTrainingTime: Long = 0

    private var wordCount: Int = 0
    private val activityRequestCode = 101

    private lateinit var progressHelper: ProgressHelper

    var trainingStatisticsData: TrainingStatisticsData? = null

    @SuppressLint("LongLogTag", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_statistics)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressHelper = ProgressHelper(this, root_view_training_statistics, listOf(share1, returnTraining))

        presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()
        val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID),-1)
        val trainingId = intent.getIntExtra(getString(R.string.CURRENT_TRAINING_ID),-1)
        if (presId > 0 && trainingId > 0) {
            presentationData = presentationDataDao?.getPresentationWithId(presId)
            trainingData = SpeechDataBase.getInstance(this)?.TrainingDataDao()?.getTrainingWithId(trainingId)
        }
        else {
            Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, "stat_act: wrong ID")
            return
        }

        printTimeOnEachSlideChart(trainingId)

        if (intent.getIntExtra(getString(R.string.launchedFromHistoryActivityFlag),-1) == launchedFromHistoryActivityFlag) returnTraining.visibility = View.GONE

        trainingSlideDBHelper = TrainingSlideDBHelper(this)
        trainingDBHelper = TrainingDBHelper(this)

        pdfReader = PdfToBitmap(presentationData!!, this)

        val trainingSlidesList = trainingSlideDBHelper?.getAllSlidesForTraining(trainingData!!) ?: return

        for (slide in trainingSlidesList)
            currentTrainingTime += slide.spentTimeInSec!!

        trainingStatisticsData = TrainingStatisticsData(this, presentationData, trainingData)

        val drawer = Thread(Runnable {
            drawPict()
        })
        drawer.start()

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

        val trainingSlideDBHelper = TrainingSlideDBHelper(this)
        val trainingSpeedData = HashMap<Int, Float>()
        val trainingSlideList = trainingSlideDBHelper.getAllSlidesForTraining(trainingData!!)

        val presentationSpeedData = mutableListOf<BarEntry>()
        for (i in 0..(trainingSlideList!!.size-1)) {
            val slide = trainingSlideList[i]
            var speed = 0f
            if (slide.knownWords != "") speed = slide.knownWords!!.split(" ").size.toFloat() / slide.spentTimeInSec!!.toFloat() * resources.getDimension(R.dimen.number_of_seconds_in_a_minute_float)
            trainingSpeedData[i] = speed
            presentationSpeedData.add(BarEntry((i).toFloat(), speed))
        }

        printSpeedLineChart(presentationSpeedData)

        val editedTextForTop10WordsChart = PrepositionsAndConjunctions(this)
                .removeConjunctionsAndPrepositionsFromText(trainingData!!.allRecognizedText)

        val presentationTop10Words = getTop10Words(editedTextForTop10WordsChart)
        val entries = ArrayList<PieEntry>()
        for (pair in presentationTop10Words){
            entries.add(PieEntry(pair.second.toFloat(), pair.first))
        }

        printPiechart(entries)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val optimalSpeed = sharedPreferences.getString(getString(R.string.speed_key), "120")
        val isExportVisible = sharedPreferences.getBoolean("deb_statistics_export", false)

        val averageSpeed = getAverageSpeed(trainingSpeedData)

        val bestSlide = getBestSlide(trainingSpeedData, optimalSpeed.toInt())
        val worstSlide = getWorstSlide(trainingSpeedData, optimalSpeed.toInt())

        if (!isExportVisible){
            export.visibility = View.VISIBLE
        }

        earnOfTrain.text = "${getString(R.string.earnings_of_training)} ${trainingStatisticsData?.trainingGrade?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} ${getString(R.string.maximum_mark_for_training)}"

        x_exercise_time_factor.append(" ${((trainingStatisticsData?.xExerciseTimeFactor)!! * resources.getInteger(R.integer.transfer_to_interest)/resources.getDimension(R.dimen.number_of_factors)).format(1)}")
        y_speech_speed_factor.append(" ${((trainingStatisticsData?.ySpeechSpeedFactor)!! * resources.getInteger(R.integer.transfer_to_interest)/resources.getDimension(R.dimen.number_of_factors)).format(1)}")
        z_time_on_slides_factor.append(" ${((trainingStatisticsData?.zTimeOnSlidesFactor)!! * resources.getInteger(R.integer.transfer_to_interest)/resources.getDimension(R.dimen.number_of_factors)).format(1)}")

        textView.text = getString(R.string.average_speed) +
                " %.2f ${getString(R.string.speech_speed_units)}\n".format(averageSpeed) +
                getString(R.string.best_slide) + " $bestSlide\n" +
                getString(R.string.worst_slide) + " $worstSlide\n" +
                getString(R.string.training_time) + " ${getStringPresentationTimeLimit(trainingStatisticsData?.currentTrainingTime)}\n" +
                getString(R.string.count_of_slides) + " ${trainingSlidesList.size}"


        speed_statistics = trainingData!!.allRecognizedText.split(" ").size
        sharedPreferences.edit().putInt(getString(R.string.num_of_words_spoken), trainingStatisticsData!!.curWordCount).putInt(getString(R.string.total_words_count), trainingStatisticsData!!.allWords).apply()
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

            tsC.drawText(getString(R.string.average_earning_1), resources.getDimension(R.dimen.x_indent_multiplier_30), resources.getDimension(R.dimen.y_indent_multiplier_232), tsP)
            tsC.drawText(getString(R.string.average_earning_2) + " " +
                    trainingStatisticsData?.averageEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score)) + " / " +
                    trainingStatisticsData?.minEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score)) + " / " +
                    trainingStatisticsData?.maxEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score)), resources.getDimension(R.dimen.x_indent_multiplier_90),
                    resources.getDimension(R.dimen.y_indent_multiplier_255), tsP)

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
                "${getString(R.string.average_earning_1)}\n\t ${getString(R.string.average_earning_2)} ${trainingStatisticsData?.averageEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} / ${trainingStatisticsData?.minEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} / ${trainingStatisticsData?.maxEarn?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))}"

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

    //Инициализация графика скорости чтения
    private fun printSpeedLineChart(lineEntries: List<BarEntry>){
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()
        val optimalSpeed = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.speed_key), "120").toString().toInt()

        for(entry in lineEntries) {
            labels.add((entry.x + 1).toInt().toString())

            colors.add(
                    when (entry.y) {
                        in optimalSpeed.toFloat() * 0.9f .. optimalSpeed.toFloat() * 1.1f -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
                        in Float.MIN_VALUE .. optimalSpeed.toFloat() * 0.9f -> ContextCompat.getColor(this, android.R.color.holo_blue_dark)
                        else -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
                    }
            )
        }

        val barDataSet = BarDataSet(lineEntries, getString(R.string.words_count))
        barDataSet.colors = colors

        val data = BarData(barDataSet)
        data.setValueTextSize(0f)

        speed_bar_chart.setTouchEnabled(false)
        speed_bar_chart.setFitBars(true)
        speed_bar_chart.data = data
        speed_bar_chart.description.text = getString(R.string.slide_number)
        speed_bar_chart.description.textSize = 15f
        speed_bar_chart.animateXY(1000,1000)
        speed_bar_chart.legend.textSize = 20f
        speed_bar_chart.legend.position = Legend.LegendPosition.ABOVE_CHART_LEFT
        speed_bar_chart.legend.formSize = 0f
        speed_bar_chart.legend.xEntrySpace = 0f


        speed_bar_chart.setTouchEnabled(false)
        speed_bar_chart.setScaleEnabled(false)//выкл возможность зумить
        speed_bar_chart.xAxis.setDrawGridLines(false)//отключение горизонтальных линии сетки
        speed_bar_chart.axisRight.isEnabled = false// ось У справа невидимая
        speed_bar_chart.axisLeft.setDrawGridLines(false)//откл вертикальных линий сетки
        speed_bar_chart.axisLeft.textSize = 15f
        speed_bar_chart.axisLeft.axisMinimum = 0f // минимальное значение оси y = 0
        speed_bar_chart.setVisibleYRangeMinimum(optimalSpeed * 1.2f, speed_bar_chart.axisLeft.axisDependency)
        speed_bar_chart.axisLeft.granularity = 20f

        val ll = LimitLine(optimalSpeed.toFloat(), getString(R.string.speech_speed))
        ll.lineWidth = 2f
        ll.lineColor = Color.GREEN
        ll.textSize = 10f
        speed_bar_chart.axisLeft.addLimitLine(ll)

        val xAxis = speed_bar_chart.xAxis
        xAxis.textSize = 12f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f

        speed_bar_chart.invalidate()
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

    private fun printTimeOnEachSlideChart(trainingId: Int) {
        val timeOnEachSlideChartFragment = TimeOnEachSlideChartFragment()
        val bundle = Bundle()

        bundle.putInt(getString(R.string.CURRENT_TRAINING_ID), trainingId)
        timeOnEachSlideChartFragment.arguments = bundle

        supportFragmentManager.beginTransaction()
                .replace(R.id.time_on_each_slide_chart_box_activity_training_statistics, timeOnEachSlideChartFragment)
                .commit()
    }

}
