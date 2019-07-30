package ru.spb.speech.fragments.statistic_fragments

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import ru.spb.speech.R
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.LargeValueFormatter
import kotlinx.android.synthetic.main.time_on_each_slide_chart_fragment.view.*
import ru.spb.speech.fragments.StatisticsFragment

const val FRAGMENT_TIME_ON_EACH_SLIDE = ".FragmentTimeOnEachSlide"

const val LOWER_RANGE_LIMIT = 0.9f
const val UPPER_RANGE_LIMIT = 1.1f


class TimeOnEachSlideFragment : StatisticsFragment() {

    companion object {
//        private const val BAR_WIDTH = 0.4f
//        private const val GROUP_SPACE = 0.2f
//        private const val BAR_SPACE = 0.0f
//        private const val START_SHIFT = -(BAR_WIDTH + GROUP_SPACE / 2)
    }

    override val fragmentLayoutId = R.layout.time_on_each_slide_chart_fragment

    override fun onViewInflated(view: View) {
        super.onViewInflated(view)

        with(view) {
            val entries = ArrayList<BarEntry>()

            val yVals: ArrayList<BarEntry> = ArrayList()

            val labels = ArrayList<String>()

            for (slide in trainingSlideList?.withIndex() ?: return) {
                entries.add(BarEntry(slide.index.toFloat(), slide.value.spentTimeInSec!!.toFloat()))

                labels.add("${slide.index + 1}")
                val pause = slide.value.silencePercentage!!.toFloat()
                val allSlide = slide.value.spentTimeInSec!!.toFloat()

                yVals.add(BarEntry(slide.index.toFloat(), floatArrayOf(pause, allSlide - pause)))
            }


            var averageTimeOnEachSlide = 0f

            for (entry in entries) {
                averageTimeOnEachSlide += entry.y.toInt()
            }
            averageTimeOnEachSlide /= entries.size

            Log.d(FRAGMENT_TIME_ON_EACH_SLIDE, "averageTime = $averageTimeOnEachSlide")

            val staeckedDataset = BarDataSet(yVals, "")
            staeckedDataset.setColors(ContextCompat.getColor(context!!, R.color.fasterThanAverageRange), ContextCompat.getColor(context!!, R.color.inTheAverageRange))


            val stackedData = BarData(staeckedDataset)

            stackedData.setValueFormatter(LargeValueFormatter())
            stackedData.setValueTextSize(0f)

            time_on_each_slide_chart.data = stackedData
            time_on_each_slide_chart.setDrawValueAboveBar(false)

            time_on_each_slide_chart.setFitBars(true)
            time_on_each_slide_chart.animateXY(1000, 1000)

//            time_on_each_slide_chart.legend.textSize = 0f
//            time_on_each_slide_chart.legend.position = Legend.LegendPosition.ABOVE_CHART_LEFT
//            time_on_each_slide_chart.legend.formSize = 0f
            time_on_each_slide_chart.legend.isEnabled = false
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

            val yAxis = time_on_each_slide_chart.axisLeft

            val averageTimeLine = LimitLine(averageTimeOnEachSlide, getString(R.string.average_time_chart))
            averageTimeLine.lineColor = Color.GREEN
            averageTimeLine.lineWidth = 2f
            averageTimeLine.textSize = 10f

            yAxis.addLimitLine(averageTimeLine)
            yAxis.setDrawLimitLinesBehindData(false)

            time_on_each_slide_chart.invalidate()
        }
    }
}
