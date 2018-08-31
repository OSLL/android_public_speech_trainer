package com.example.company.myapplication

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
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

        val barDataSet = BarDataSet(lineEntries, "Количество слов в минуту")
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS,255)

        val data = BarData(barDataSet)

        speed_bar_chart.setFitBars(true)
        speed_bar_chart.setData(data)
        speed_bar_chart.description.text = "Номер слайда"
        speed_bar_chart.description.setTextSize(10f)
        speed_bar_chart.animateXY(1000,1000)
        speed_bar_chart.legend.setTextSize(12f)

        val xAxis = speed_bar_chart.xAxis
        xAxis.textSize = 12f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setValueFormatter(IndexAxisValueFormatter(labels))

        speed_bar_chart.invalidate()
    }
}
