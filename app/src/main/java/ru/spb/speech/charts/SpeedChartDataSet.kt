package ru.spb.speech.charts

import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry



class SpeedChartDataSet: BarDataSet {

    constructor(yVals: List<BarEntry>?, label: String?) : super(yVals, label)

    override fun getEntryIndex(e: BarEntry?): Int {
        return getEntryIndex(e)
    }


    override fun getColor(index: Int): Int {
        return mColors[index]
    }
}