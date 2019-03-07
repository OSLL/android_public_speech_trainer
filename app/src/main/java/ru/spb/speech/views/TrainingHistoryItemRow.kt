package ru.spb.speech.views

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.View
import ru.spb.speech.DBTables.helpers.TrainingSlideDBHelper
import ru.spb.speech.R
import ru.spb.speech.DBTables.TrainingData
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.training_history_row.view.*


class TrainingHistoryItemRow(private val training: TrainingData, private val slidesCount: Int, private val ctx: Context): Item<ViewHolder>() {
    var trainingId: Int? = null
    var trainingEndFlag: Boolean = true

    override fun getLayout(): Int {
        return R.layout.training_history_row
    }

    @SuppressLint("SetTextI18n")
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.date_training_history_row.text = DateUtils.formatDateTime(
                ctx, training.timeStampInSec!! * 1000, DateUtils.FORMAT_SHOW_DATE) + " | " +
                DateUtils.formatDateTime(
                        ctx, training.timeStampInSec!! * 1000, DateUtils.FORMAT_SHOW_TIME)

        val helper = TrainingSlideDBHelper(ctx)
        val slides = helper.getAllSlidesForTraining(training)

        if (slides != null && slides.count() < slidesCount) {
            trainingEndFlag = false
            viewHolder.itemView.training_not_end_view_training_history_row.visibility = View.VISIBLE
            viewHolder.itemView.background= ctx.getDrawable(R.drawable.training_not_end_item_background)
        }

        trainingId = training.id
    }

}