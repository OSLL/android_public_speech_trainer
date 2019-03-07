package ru.spb.speech

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import ru.spb.speech.DBTables.helpers.TrainingDBHelper
import ru.spb.speech.appSupport.ProgressHelper
import ru.spb.speech.views.TrainingHistoryItemRow
import ru.spb.speech.DBTables.PresentationData
import ru.spb.speech.DBTables.SpeechDataBase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_training_history.*

const val ACTIVITY_HISTORY_NAME = ".TrainingHistoryActivity"

class TrainingHistoryActivity : AppCompatActivity() {
    companion object {
        const val launchedFromHistoryActivityFlag = 1
    }

    private lateinit var progressHelper: ProgressHelper
    private var presentationData: PresentationData? = null

    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        progressHelper = ProgressHelper(this, root_view_training_history, listOf(recyclerview_training_history))

        val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID),-1)
        if (presId > 0) {
            presentationData =SpeechDataBase.getInstance(this)?.PresentationDataDao()?.getPresentationWithId(presId)
        }
        else {
            Log.d(APST_TAG + ACTIVITY_HISTORY_NAME, "stat_act: wrong ID")
            return
        }

        val trainingDBHelper = TrainingDBHelper(this)
        val list = trainingDBHelper.getAllTrainingsForPresentation(presentationData!!) ?: return
        val adapter = GroupAdapter<ViewHolder>()

        for (training in list) {
            adapter.add(TrainingHistoryItemRow(training, presentationData?.pageCount!!, this))
        }

        recyclerview_training_history.adapter = adapter

        adapter.setOnItemClickListener{ item: Item<ViewHolder>, view: View ->
            val row = item as TrainingHistoryItemRow
            val i = Intent(this, TrainingStatisticsActivity::class.java)
            i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), presentationData?.id)
            i.putExtra(getString(R.string.CURRENT_TRAINING_ID), row.trainingId)
            i.putExtra(getString(R.string.launchedFromHistoryActivityFlag), launchedFromHistoryActivityFlag)

            if (!row.trainingEndFlag) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(getString(R.string.you_dont_end_training))
                builder.setPositiveButton(getString(R.string.continue_training)) { _, _ ->
                    Toast.makeText(this, "coming soon...", Toast.LENGTH_LONG).show()
                }

                builder.setNegativeButton(getString(R.string.go_to_statistics)) { _, _ ->
                    startActivity(i)
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
            else {
                startActivity(i)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            super.onBackPressed()
            return true
        }
        return false
    }

    override fun onPause() {
        progressHelper.show()
        super.onPause()
    }

    override fun onResume() {
        progressHelper.hide()
        super.onResume()
    }
}
