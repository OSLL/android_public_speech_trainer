package com.example.company.myapplication

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.activity_training_statistics.*

class TrainingStatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_statistics)

        //Тестовые данные для speed line chart
        val presentationSpeedData = mutableListOf<BarEntry>()
        presentationSpeedData.add(BarEntry(0f,20f))
        presentationSpeedData.add(BarEntry(1f,10f))
        presentationSpeedData.add(BarEntry(2f,2f))
        presentationSpeedData.add(BarEntry(3f,56f))
        presentationSpeedData.add(BarEntry(4f,0f))
        //------------

        printSpeedLineChart(presentationSpeedData)
    }

    //Инициализация графика скорсти чтения
    fun printSpeedLineChart(lineEntries: List<BarEntry>){
        val labels = ArrayList<String>()

        for(entry in lineEntries)
            labels.add(entry.x.toInt().toString())

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
}
