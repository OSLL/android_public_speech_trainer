package com.example.company.myapplication

import android.content.Intent
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.support.v7.app.AppCompatActivity
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.activity_training_statistics.*
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry



class TrainingStatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_statistics)
        //share example
        share1.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val shareBody = "Your body here"
            val shareSub = "Your subject here"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSub)
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
            startActivity(Intent.createChooser(sharingIntent, "Share using"))
        }
        //-------------

        val presentationEntries = intent.getSerializableExtra(getString(R.string.presentationEntries)) as HashMap<Int,Float?>
        val presentationSpeedData = mutableListOf<BarEntry>()
        for (i in 0..(presentationEntries.size-1)) {
            presentationSpeedData.add(BarEntry((i).toFloat(), presentationEntries.get(i + 1)!!.toFloat()))
        }
        printSpeedLineChart(presentationSpeedData)

        val entries = ArrayList<PieEntry>()

        entries.add(PieEntry(18.5f, "Green"))
        entries.add(PieEntry(26.7f, "Yellow"))
        entries.add(PieEntry(24.0f, "Red"))
        entries.add(PieEntry(30.8f, "Blue"))
        printPiechart(entries)
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
        speed_bar_chart.description.setTextSize(15f)
        speed_bar_chart.animateXY(1000,1000)
        speed_bar_chart.legend.setTextSize(20f)
        speed_bar_chart.legend.setPosition(Legend.LegendPosition.ABOVE_CHART_LEFT)
        speed_bar_chart.legend.formSize = 0f
        speed_bar_chart.legend.xEntrySpace = 0f


        speed_bar_chart.setScaleEnabled(false)//выкл возможность зумить
        speed_bar_chart.getXAxis().setDrawGridLines(false)//отключение горизонтальных линии сетки
        speed_bar_chart.axisRight.isEnabled = false// ось У справа невидимая
        speed_bar_chart.axisLeft.setDrawGridLines(false)//откл вертикальных линий сетки
        speed_bar_chart.axisLeft.textSize = 15f

        val xAxis = speed_bar_chart.xAxis
        xAxis.textSize = 12f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        speed_bar_chart.invalidate()
    }

    fun printPiechart (lineEntries: List<PieEntry>){
//        val labels = ArrayList<String>()
//        for(entry in lineEntries)
//            labels.add((entry.x +1).toInt().toString())
        val pieDataSet = PieDataSet(lineEntries, getString(R.string.words_count))
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS,255)
//        val entries = ArrayList<PieEntry>()
//
//        val set = PieDataSet(entries, "Election Results")
        val data = PieData(pieDataSet)
        pie_chart.data = data

        pie_chart.invalidate()
    }
}
