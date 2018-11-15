package com.example.company.myapplication.views

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import com.example.company.myapplication.R
import com.example.putkovdimi.trainspeech.DBTables.TrainingData
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.training_history_row.view.*


class TrainingHistoryItem(private val training: TrainingData, private val ctx: Context): Item<ViewHolder>() {
    var trainingId: Int? = null

    override fun getLayout(): Int {
        return R.layout.training_history_row
    }

    @SuppressLint("SetTextI18n")
    override fun bind(viewHolder: ViewHolder, position: Int) {
        //viewHolder.itemView.date_training_history_row.text = getDateCurrentTimeZone(training.timeStampInSec!!)
        viewHolder.itemView.date_training_history_row.text = DateUtils.formatDateTime(
                ctx, training.timeStampInSec!! * 1000, DateUtils.FORMAT_SHOW_DATE) + " | " +
                DateUtils.formatDateTime(
                        ctx, training.timeStampInSec!! * 1000, DateUtils.FORMAT_SHOW_TIME)

        trainingId = training.id
    }

}