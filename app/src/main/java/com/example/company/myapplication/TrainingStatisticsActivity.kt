package com.example.company.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.company.myapplication.DBTables.helpers.TrainingDBHelper
import com.example.company.myapplication.DBTables.helpers.TrainingSlideDBHelper
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.example.putkovdimi.trainspeech.DBTables.TrainingData
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.activity_training_statistics.*
import java.text.BreakIterator
import java.util.*
import java.util.concurrent.TimeUnit

var bmpBase: Bitmap? = null
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

    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_statistics)

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

        trainingSlideDBHelper = TrainingSlideDBHelper(this)
        trainingDBHelper = TrainingDBHelper(this)

        share1.setOnClickListener {
            try {
                DrawPict()
                url = MediaStore.Images.Media.insertImage(this.contentResolver, finishBmp, "title", null)

            }catch (e: Exception) {
                Log.d(APST_TAG + ACTIVITY_TRAINING_STATISTIC_NAME, e.toString())
            }
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.putExtra(Intent.EXTRA_STREAM,  Uri.parse(url))
            sharingIntent.type = "image/jpg"
            startActivity(Intent.createChooser(sharingIntent, "Share with friends"))
        }

        returnBut.setOnClickListener{
            val returnIntent = Intent(this, StartPageActivity::class.java)
            startActivity(returnIntent)
        }

        val trainingSlideList = trainingSlideDBHelper?.getAllSlidesForTraining(trainingData!!)

        val presentationSpeedData = mutableListOf<BarEntry>()
        for (i in 0..(trainingSlideList!!.size-1)) {
            val slide = trainingSlideList[i]
            var speed = 0f
            if (slide.knownWords != "") speed = slide.knownWords!!.split(" ").size.toFloat() / slide.spentTimeInSec!!.toFloat() * 60f
            presentationSpeedData.add(BarEntry((i).toFloat(), speed))
        }

        printSpeedLineChart(presentationSpeedData)


        val presentationTop10Words = getTop10Words(trainingData!!.allRecognizedText)
        val entries = ArrayList<PieEntry>()
        for (pair in presentationTop10Words){
            entries.add(PieEntry(pair.second.toFloat(), pair.first))
        }
        printPiechart(entries)

        speed_statistics = trainingData!!.allRecognizedText.split(" ").size
    }

    fun DrawPict() {
        val trainingsList = trainingDBHelper?.getAllTrainingsForPresentation(presentationData!!) ?: return
        val trainingSlidesList = trainingSlideDBHelper?.getAllSlidesForTraining(trainingData!!) ?: return

        val trainingCount = trainingsList.size
        var currentTrainingTime: Long = 0

        for (slide in trainingSlidesList)
            currentTrainingTime += slide.spentTimeInSec!!

        val width = bmpBase?.width
        val height = bmpBase?.height
        val presName = presentationData?.name

        if(width != null && height != null) {
            val NWidth: Int = width
            val NHeight: Int = height
            finishBmp = Bitmap.createBitmap(NWidth, NHeight + 185, Bitmap.Config.ARGB_8888)

            val whitePaint = Paint()
            whitePaint.style = Paint.Style.FILL
            whitePaint.color = Color.WHITE

            val nameBmp = Bitmap.createBitmap(NWidth, 40, Bitmap.Config.ARGB_8888)
            val nameC = Canvas(nameBmp)
            nameC.drawPaint(whitePaint)
            val namePaint = Paint()
            namePaint.color = Color.BLACK
            namePaint.style = Paint.Style.FILL
            namePaint.isAntiAlias = true
            if(presName?.length != null) {
                when {
                    presName.length < 32 -> namePaint.textSize = 24f
                    presName.length < 37 -> namePaint.textSize = 20f
                    else -> namePaint.textSize = 16f
                }
                namePaint.isUnderlineText = true
                if (presName.length < 30) {
                    nameC.drawText(presName, ((32 - presName.length).toFloat()) * 6.5f, 30f, namePaint)
                } else
                    nameC.drawText(presName, 20f, 30f, namePaint)
            }


            val countBmp = Bitmap.createBitmap(NWidth, 30, Bitmap.Config.ARGB_8888)
            val countC = Canvas(countBmp)
            countC.drawPaint(whitePaint)
            val countPaint = Paint()
            countPaint.color = Color.BLACK
            countPaint.style = Paint.Style.FILL
            countPaint.isAntiAlias = true
            countPaint.textSize = 20f
            countC.drawText(getString(R.string.count_of_training) + getCase(trainingCount, "раз", "раза", "раз"), 20f, 20f, countPaint)

            val statBmp = Bitmap.createBitmap(NWidth, 115, Bitmap.Config.ARGB_8888)
            val statC = Canvas(statBmp)
            statC.drawPaint(whitePaint)
            val statPaint = Paint()
            statPaint.color = Color.BLACK
            statPaint.style = Paint.Style.FILL
            statPaint.isAntiAlias = true
            statPaint.textSize = 20f
            statC.drawText(getString(R.string.result_of_training), 20f, 20f, statPaint)
            statPaint.textSize = 17f
            statPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            statC.drawText(getString(R.string.time_of_training) + getStringPresentationTimeLimit(currentTrainingTime), 30f, 43f, statPaint)
            statC.drawText(getString(R.string.record_of_training), 30f, 66f, statPaint)
            statC.drawText(getString(R.string.earnings_of_training), 30f, 99f, statPaint)

            val canvas = Canvas(finishBmp)
            val paint = Paint()
            canvas.drawBitmap(bmpBase, 0f, 0f, paint)
            canvas.drawBitmap(nameBmp, 0f, NHeight.toFloat(), paint)
            canvas.drawBitmap(countBmp, 0f, NHeight.toFloat() + 40f, paint)
            canvas.drawBitmap(statBmp, 0f, NHeight.toFloat() + 70f, paint)
            val paintCircle = Paint()
            paintCircle.color = Color.YELLOW
            canvas.drawCircle(185f, NHeight.toFloat() + 161f, 15f, paintCircle)
            canvas.drawCircle(225f, NHeight.toFloat() + 161f, 15f, paintCircle)
            canvas.drawCircle(265f, NHeight.toFloat() + 161f, 15f, paintCircle)
        }
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

        val min = getCase(minutes.toInt(), "минуту", "минуты", "минут")
        val sec = getCase(seconds.toInt(), "секунду", "секунды", "секунд")

        val res = String.format(
                Locale.getDefault(),
                "%01d $min %01d $sec",
                minutes, seconds
        )

        if(minutes.toInt() == 0){
            return " ${res.substring(res.indexOf("с") - 3)}"
        } else {
            return " ${res.substring(res.indexOf("м") - 3, res.indexOf("м") + 6) + res.substring(res.indexOf("с") - 3)}"
        }
    }

    //Инициализация графика скорсти чтения
    fun printSpeedLineChart(lineEntries: List<BarEntry>){
        val labels = ArrayList<String>()

        for(entry in lineEntries)
            labels.add((entry.x +1).toInt().toString())

        val barDataSet = BarDataSet(lineEntries, getString(R.string.words_count))
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS,255)

        val data = BarData(barDataSet)
        data.setValueTextSize(0f)

        speed_bar_chart.setFitBars(true)
        speed_bar_chart.data = data
        speed_bar_chart.description.text = getString(R.string.slide_number)
        speed_bar_chart.description.textSize = 15f
        speed_bar_chart.animateXY(1000,1000)
        speed_bar_chart.legend.textSize = 20f
        speed_bar_chart.legend.position = Legend.LegendPosition.ABOVE_CHART_LEFT
        speed_bar_chart.legend.formSize = 0f
        speed_bar_chart.legend.xEntrySpace = 0f


        speed_bar_chart.setScaleEnabled(false)//выкл возможность зумить
        speed_bar_chart.xAxis.setDrawGridLines(false)//отключение горизонтальных линии сетки
        speed_bar_chart.axisRight.isEnabled = false// ось У справа невидимая
        speed_bar_chart.axisLeft.setDrawGridLines(false)//откл вертикальных линий сетки
        speed_bar_chart.axisLeft.textSize = 15f
        speed_bar_chart.axisLeft.axisMinimum = 0f // минимальное значение оси y = 0

        val xAxis = speed_bar_chart.xAxis
        xAxis.textSize = 12f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f

        speed_bar_chart.invalidate()
    }

    fun printPiechart (lineEntries: List<PieEntry>){

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

    fun getTop10Words(text: String) : List<Pair<String, Int>> {
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
