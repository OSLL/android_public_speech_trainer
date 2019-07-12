package ru.spb.speech.fragments

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

const val FRAGMENT_TIME_ON_EACH_SLIDE = ".FragmentTimeOnEachSlide"

const val LOWER_RANGE_LIMIT = 0.9f
const val UPPER_RANGE_LIMIT = 1.1f


class TimeOnEachSlideFragment : StatisticsFragment() {

    companion object {
        private const val BAR_WIDTH = 0.4f
        private const val GROUP_SPACE = 0.2f
        private const val BAR_SPACE = 0.0f
        private const val START_SHIFT = -(BAR_WIDTH + GROUP_SPACE / 2)
    }

    override val fragmentLayoutId = R.layout.time_on_each_slide_chart_fragment

    override fun onViewInflated(view: View) {
        super.onViewInflated(view)

        with(view) {
            val entries = ArrayList<BarEntry>()
            val pauseEntries = ArrayList<BarEntry>()

            for (slide in trainingSlideList?.withIndex() ?: return) {
                entries.add(BarEntry(slide.index.toFloat(), slide.value.spentTimeInSec!!.toFloat()))
                pauseEntries.add(BarEntry(slide.index.toFloat(), slide.value.silencePercentage!!.toFloat()))
            }

            val labels = ArrayList<String>()
            val colors = ArrayList<Int>()

            var averageTimeOnEachSlide = 0
            for (entry in entries) {
                averageTimeOnEachSlide += entry.y.toInt()
            }
            averageTimeOnEachSlide /= entries.size
            Log.d(FRAGMENT_TIME_ON_EACH_SLIDE, "averageTime = $averageTimeOnEachSlide")

            for (entry in entries) {
                labels.add((entry.x + 1).toInt().toString())

                colors.add(
                        when (entry.y) {
                            in averageTimeOnEachSlide * LOWER_RANGE_LIMIT..averageTimeOnEachSlide * UPPER_RANGE_LIMIT -> ContextCompat.getColor(context!!, R.color.inTheAverageRange)
                            in Float.MIN_VALUE..averageTimeOnEachSlide * LOWER_RANGE_LIMIT -> ContextCompat.getColor(context!!, R.color.slowerThanAverageRange)
                            else -> ContextCompat.getColor(context!!, R.color.fasterThanAverageRange)
                        }
                )
            }

//            val displayMetrics = DisplayMetrics()
//            (context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager).getDefaultDisplay().getMetrics(displayMetrics)
//            val width = displayMetrics.widthPixels

//            var barDataSet = BarDataSet(entries, getString(R.string.slideDurationInSeconds))
            val barDataSet = BarDataSet(entries, "")
            val pauseBarDataSet = BarDataSet(pauseEntries, "")

//            if(width < resources.getInteger(R.integer.screen_width)){
//                barDataSet = BarDataSet(entries, getString(R.string.slideDurationInSeconds_max1080))
//            }

            barDataSet.colors = colors
            pauseBarDataSet.color = ContextCompat.getColor(context!!, android.R.color.black)

            val data = BarData(pauseBarDataSet, barDataSet)
            data.setValueFormatter(LargeValueFormatter())
            data.setValueTextSize(0f)

            time_on_each_slide_chart.data = data
            time_on_each_slide_chart.data.barWidth = BAR_WIDTH
            time_on_each_slide_chart.groupBars(START_SHIFT, GROUP_SPACE, BAR_SPACE)

            time_on_each_slide_chart.setFitBars(true)
            time_on_each_slide_chart.animateXY(1000, 1000)

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

            val yAxis = time_on_each_slide_chart.axisLeft

            val averageTimeLine = LimitLine(averageTimeOnEachSlide.toFloat(), getString(R.string.average_time_chart))
            averageTimeLine.lineColor = Color.GREEN
            averageTimeLine.lineWidth = 2f
            averageTimeLine.textSize = 10f

            yAxis.addLimitLine(averageTimeLine)
            yAxis.setDrawLimitLinesBehindData(true)

            time_on_each_slide_chart.invalidate()
        }
    }
}
