package com.example.putkovdimi.trainspeech

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.example.putkovdimi.trainspeech.DBTables.DBWorkerThread
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate


class MainActivity : AppCompatActivity() {

    private var mDb: SpeechDataBase? = null

    private lateinit var mDbWorkerThread: DBWorkerThread

    private val mUiHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDbWorkerThread = DBWorkerThread("dbWorkerThread")
        mDbWorkerThread.start()

        mDb = SpeechDataBase.getInstance(this)
        var presentation = PresentationData()
        presentation.name = "ex1"
        presentation.path = "path1"
        insertPresentationDataInDb(presentation)
        print(mDb?.PresentationDataDao()?.getAll())
    }


    private fun insertPresentationDataInDb(presentationData: PresentationData) {
        val task = Runnable { mDb?.PresentationDataDao()?.insert(presentationData) }
        mDbWorkerThread.postTask(task)
    }
}