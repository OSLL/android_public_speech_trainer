package com.example.company.myapplication.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.company.myapplication.APST_TAG
import com.example.company.myapplication.DBTables.helpers.TrainingSlideDBHelper
import com.example.company.myapplication.R
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.time_on_each_slide_chart_fragment.*

const val FRAGMENT_TIME_ON_EACH_SLIDE = ".FragmentTimeOnEachSlide"

class TimeOnEachSlideChartFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.time_on_each_slide_chart_fragment, container, false)
    }

    @SuppressLint("LongLogTag")
    override fun onStart() {
        super.onStart()

        val bundle = this.arguments ?: return
        val trainingId = bundle.getInt(getString(R.string.CURRENT_TRAINING_ID))

        val training = SpeechDataBase.getInstance(activity!!)?.TrainingDataDao()?.getTrainingWithId(trainingId)
        if (training == null) { Log.d(APST_TAG + FRAGMENT_TIME_ON_EACH_SLIDE, "training == null"); return }

        val trainingList = TrainingSlideDBHelper(activity!!).getAllSlidesForTraining(training)
        if (trainingList == null) { Log.d(APST_TAG + FRAGMENT_TIME_ON_EACH_SLIDE, "trainingList == null"); return }

        val entries = ArrayList<BarEntry>()
        
        for (i in 0..(trainingList.size - 1)) {
            entries.add(BarEntry(i.toFloat(), trainingList[i].spentTimeInSec!!.toFloat()))
        }

        printChart(entries)
    }

    private fun printChart(entries: ArrayList<BarEntry>) {

        val labels = java.util.ArrayList<String>()

        for(entry in entries)
            labels.add((entry.x +1).toInt().toString())

        val barDataSet = BarDataSet(entries, getString(R.string.slideDurationInSeconds))
        barDataSet.color = R.color.timeOncEachSlideChartColor

        val data = BarData(barDataSet)
        data.setValueTextSize(0f)

        time_on_each_slide_chart.setFitBars(true)
        time_on_each_slide_chart.data = data
        time_on_each_slide_chart.animateXY(1000,1000)

        time_on_each_slide_chart.legend.textSize = 20f
        time_on_each_slide_chart.legend.position = Legend.LegendPosition.ABOVE_CHART_LEFT
        time_on_each_slide_chart.legend.formSize = 0f
        time_on_each_slide_chart.legend.xEntrySpace = 0f
        time_on_each_slide_chart.description.text = getString(R.string.slide_number)
        time_on_each_slide_chart.description.textSize = 15f

        time_on_each_slide_chart.setTouchEnabled(false)
        time_on_each_slide_chart.setScaleEnabled(false)//выкл возможность зумить
        time_on_each_slide_chart.xAxis.setDrawGridLines(false)//отключение горизонтальных линии сетки
        time_on_each_slide_chart.axisRight.isEnabled = false// ось У справа невидимая
        time_on_each_slide_chart.axisLeft.setDrawGridLines(false)//откл вертикальных линий сетки

        time_on_each_slide_chart.axisLeft.textSize = 15f
        time_on_each_slide_chart.axisLeft.axisMinimum = 0f // минимальное значение оси y = 0

        val xAxis = time_on_each_slide_chart.xAxis
        xAxis.textSize = 12f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f

        time_on_each_slide_chart.invalidate()
    }

}
