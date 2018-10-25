package com.example.company.myapplication

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.activity_training_statistics.*
import java.text.BreakIterator

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


        val presentationTop10Words = getTop10Words(intent.getStringExtra("allRecognizedText"))
        val entries = ArrayList<PieEntry>()
        for (pair in presentationTop10Words){
            entries.add(PieEntry(pair.second.toFloat(), pair.first))
        }
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
        speed_bar_chart.setData(data)
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
        xAxis.setValueFormatter(IndexAxisValueFormatter(labels))

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
