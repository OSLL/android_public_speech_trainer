package com.example.company.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.company.myapplication.DBTables.helpers.TrainingDBHelper
import com.example.company.myapplication.views.TrainingHistoryItem
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_training_history.*

const val ACTIVITY_HISTORY_NAME = ".TrainingHistoryActivity"

class TrainingHistoryActivity : AppCompatActivity() {
    private var presentationData: PresentationData? = null

    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_history)

        val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID),-1)
        if (presId > 0) {
            presentationData =SpeechDataBase.getInstance(this)?.PresentationDataDao()?.getPresentationWithId(presId)
        }
        else {
            Log.d(TEST_DB + ACTIVITY_HISTORY_NAME, "stat_act: wrong ID")
            return
        }

        val trainingDBHelper = TrainingDBHelper(this)
        val list = trainingDBHelper.getAllTrainingsForPresentation(presentationData!!) ?: return
        val adapter = GroupAdapter<ViewHolder>()

        for (training in list) {
            adapter.add(TrainingHistoryItem(training, this))
        }

        recyclerview_training_history.adapter = adapter

        adapter.setOnItemClickListener{ item: Item<ViewHolder>, view: View ->
            val row = item as TrainingHistoryItem
            val i = Intent(this, TrainingStatisticsActivity::class.java)
            i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), presentationData?.id)
            i.putExtra(getString(R.string.CURRENT_TRAINING_ID), row.trainingId)
            startActivity(i)
        }
    }
}
