package ru.spb.speech.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import ru.spb.speech.DBTables.PresentationData
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.presentation_startpage_row.view.*
import java.util.*
import java.util.concurrent.TimeUnit
import android.util.Log
import android.view.View
import ru.spb.speech.*
import java.lang.Exception
import java.text.SimpleDateFormat


class PresentationStartpageItemRow(private var presentation: PresentationData, private var firstPageBitmap: Bitmap?, private val ctx: Context): Item<ViewHolder>() {
    companion object {
        const val activatedChangePresentationFlag = 1
    }

    var presentationId: Int? = null
    var presentationName: String? = null
    var presentationTimeLimit: Long? = null

    var presentationUri: String? = null
    var presentationDate: String? = null


    override fun getLayout(): Int {
        return R.layout.presentation_startpage_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.name_presentation_start_page_row.text = presentation.name
        viewHolder.itemView.time_limit_presentation_start_page_row.text = getStringPresentationTimeLimit(presentation.timeLimit!!)
        viewHolder.itemView.page_count_presentation_start_page_row.text = getStringPresentationPageCount(presentation.pageCount)
        viewHolder.itemView.image_view_presentation_start_page_row.setImageBitmap(firstPageBitmap)

        val date = getStringPresentationDate(presentation.presentationDate)
        if (date != "") {
            viewHolder.itemView.presentation_date_layout_start_page_row.visibility = View.VISIBLE
            viewHolder.itemView.presentation_date_start_page_row.text = date
        }
        else {
            viewHolder.itemView.presentation_date_layout_start_page_row.visibility = View.INVISIBLE
        }

        presentationId = presentation.id
        presentationName = presentation.name
        presentationTimeLimit = presentation.timeLimit

        presentationUri = presentation.stringUri
        presentationDate = presentation.presentationDate

        viewHolder.itemView.training_history_btn_start_page_row.setOnClickListener {
            try {
                val i = Intent(ctx, TrainingHistoryActivity::class.java)
                i.putExtra(ctx.getString(R.string.CURRENT_PRESENTATION_ID), presentationId)
                ctx.startActivity(i)
            } catch (e: Exception) {
                Log.d("adapter_test", "row err: $e")
            }
        }
    }

    fun setPresentationData(pd: PresentationData) {
        this.presentation = pd
    }

    fun setBitmap(bm: Bitmap) {
        this.firstPageBitmap = bm
    }

    @SuppressLint("UseSparseArrays")
    private fun getStringPresentationTimeLimit(t: Long?): String {
        if (t == null)
            return "undefined"

        var millisUntilFinishedVar: Long = t


        val minutes = TimeUnit.SECONDS.toMinutes(millisUntilFinishedVar)
        millisUntilFinishedVar -= TimeUnit.MINUTES.toSeconds(minutes)

        val seconds = millisUntilFinishedVar

        return String.format(
                Locale.getDefault(),
                "%02d min: %02d sec",
                minutes, seconds
        )
    }

    private fun getStringPresentationPageCount(n: Int?): String {
        if (n == null || n <= 0) {
            return "undefined"
        }

        val titles = arrayOf("$n слайд","$n слайда","$n слайдов")
        val cases = arrayOf(2, 0, 1, 1, 1, 2)

        return titles[if (n % 100 in 5..19) 2 else cases[if (n % 10 < 5) n % 10 else 5]]
    }

    private fun getStringPresentationDate(date: String): String {
        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateParser.parse(dateParser.format(Date()))
        val presDate = dateParser.parse(date)

        return if (currentDate.before(presDate) || currentDate == presDate) {
            date
        }
        else {
            ""
        }
    }
}