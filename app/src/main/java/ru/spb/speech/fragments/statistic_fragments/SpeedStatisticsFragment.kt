package ru.spb.speech.fragments.statistic_fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.View
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.fragment_speed_statistics.view.*
import ru.spb.speech.R
import ru.spb.speech.fragments.StatisticsFragment
import java.util.HashMap

class SpeedStatisticsFragment: StatisticsFragment() {
    override val fragmentLayoutId = R.layout.fragment_speed_statistics

    @SuppressLint("UseSparseArrays")
    override fun onViewInflated(view: View) {
        super.onViewInflated(view)

        trainingSlideList?:return

        val trainingSpeedData = HashMap<Int, Float>()
        val lineEntries = mutableListOf<BarEntry>()

        for (i in 0 until trainingSlideList!!.size) {
            val slide = trainingSlideList!![i]
            var speed = 0f
            if (slide.knownWords != "") speed = slide.knownWords!!.split(" ").size.toFloat() / slide.spentTimeInSec!!.toFloat() * resources.getDimension(R.dimen.number_of_seconds_in_a_minute_float)
            trainingSpeedData[i] = speed
            lineEntries.add(BarEntry((i).toFloat(), speed))
        }

        with (view) {
            val labels = ArrayList<String>()
            val colors = ArrayList<Int>()
            val optimalSpeed = PreferenceManager
                    .getDefaultSharedPreferences(activity)
                    .getString(getString(R.string.speed_key), "120")
                    .toString().toInt()

            for(entry in lineEntries) {
                labels.add((entry.x + 1).toInt().toString())

                colors.add(
                        when (entry.y) {
                            in optimalSpeed.toFloat() * 0.9f .. optimalSpeed.toFloat() * 1.1f -> ContextCompat.getColor(activity!!, android.R.color.holo_green_dark)
                            in Float.MIN_VALUE .. optimalSpeed.toFloat() * 0.9f -> ContextCompat.getColor(activity!!, android.R.color.holo_blue_dark)
                            else -> ContextCompat.getColor(activity!!, android.R.color.holo_red_dark)
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

    }
}