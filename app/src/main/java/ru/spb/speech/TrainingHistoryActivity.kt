package ru.spb.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import ru.spb.speech.database.helpers.TrainingDBHelper
import ru.spb.speech.appSupport.ProgressHelper
import ru.spb.speech.views.TrainingHistoryItemRow
import ru.spb.speech.database.PresentationData
import ru.spb.speech.database.SpeechDataBase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_training_history.*
import ru.spb.speech.database.TrainingData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

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

        export_training_history.setOnClickListener {
            if (!checkStoragePermission()) {
                checkStoragePermission()
                Toast.makeText(this, getString(R.string.try_again), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val trainingsFile: File?
            val sdState = android.os.Environment.getExternalStorageState()
            trainingsFile = if (sdState == android.os.Environment.MEDIA_MOUNTED) {
                val sdDir = android.os.Environment.getExternalStorageDirectory()
                File(sdDir, getString(R.string.training_statistics_directory))
            } else {
                this.cacheDir
            }
            if (!trainingsFile!!.exists())
                trainingsFile.mkdir()

            val curTrainingFile: File?
            curTrainingFile = if (sdState == android.os.Environment.MEDIA_MOUNTED) {
                val sdDir = android.os.Environment.getExternalStorageDirectory()
                File(sdDir, "${getString(R.string.training_statistics_directory)}/${presentationData?.name}")
            } else {
                this.cacheDir
            }
            if (!curTrainingFile!!.exists())
                curTrainingFile.mkdir()

            try {
                val textFile = File(Environment.getExternalStorageDirectory(), "${getString(R.string.training_statistics_directory)}/${presentationData?.name}/${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())}.txt")
                val fos = FileOutputStream(textFile)
                fos.write(trainingsStatInTxtFormat(list).toByteArray())
                fos.close()
                Toast.makeText(this, "${getString(R.string.successful_export_statistics)} ${getString(R.string.training_statistics_directory)}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.d(ACTIVITY_TRAINING_STATISTIC_NAME, getString(R.string.error_creating_text_file))
                Toast.makeText(this, getString(R.string.error_export_statistics), Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    fun Float.format(digits: Int) = java.lang.String.format("%.${digits}f", this)!!

    private fun currentTrainStatInTxtFormat(trainingStatisticsData: TrainingStatisticsData?):String {
        return  "${getString(R.string.date_and_time_to_start_training)} ${trainingStatisticsData?.dateOfCurTraining}\n" +
                "${getString(R.string.worked_out_a_slide)} ${trainingStatisticsData?.curSlides} / ${trainingStatisticsData?.slides}\n" +
                "${getString(R.string.time_limit_training)} ${getStringPresentationTimeLimit(trainingStatisticsData?.reportTimeLimit)}\n" +
                "${getString(R.string.num_of_words_spoken)} ${trainingStatisticsData?.curWordCount}\n" +
                "${getString(R.string.training_duration)} ${getStringPresentationTimeLimit(trainingStatisticsData?.currentTrainingTime)}\n" +
                "${getString(R.string.earnings_of_training)} ${trainingStatisticsData?.trainingGrade?.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} ${getString(R.string.maximum_mark_for_training)}\n\n"
    }

    private fun trainingsStatInTxtFormat(trainingList: MutableList<TrainingData>): String {
        var textStatistics = "${getString(R.string.name_of_pres)} ${presentationData?.name}\n\n"
        for (training in trainingList) {
            textStatistics += currentTrainStatInTxtFormat(TrainingStatisticsData(this, presentationData, training))
        }

        val trainingStatisticsData = TrainingStatisticsData(this, presentationData, trainingList[0])

        return textStatistics + "\t${getString(R.string.training_statistic_title)}\n" +
                "${getString(R.string.date_of_first_training)} ${trainingStatisticsData.dateOfFirstTraining}\n" +
                "${getString(R.string.training_completeness)} ${trainingStatisticsData.countOfCompleteTraining} / ${trainingStatisticsData.trainingCount}\n" +
                "${getString(R.string.getting_into_the_regulations)} ${trainingStatisticsData.fallIntoReg} / ${trainingStatisticsData.trainingCount}\n" +
                "${getString(R.string.mean_deviation_from_the_limit)} ${getStringPresentationTimeLimit(trainingStatisticsData.averageExtraTime)}\n" +
                "${getString(R.string.max_training_time)} ${getStringPresentationTimeLimit(trainingStatisticsData.maxTrainTime)}\n" +
                "${getString(R.string.min_training_time)} ${getStringPresentationTimeLimit(trainingStatisticsData.minTrainTime)}\n" +
                "${getString(R.string.average_time)} ${getStringPresentationTimeLimit(trainingStatisticsData.averageTime)}\n" +
                "${getString(R.string.total_words_count)} ${trainingStatisticsData.allWords}\n" +
                "${getString(R.string.average_earning_1)}\n\t ${getString(R.string.average_earning_2)} ${trainingStatisticsData.averageEarn.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} / ${trainingStatisticsData.minEarn.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))} / ${trainingStatisticsData.maxEarn.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score))}"
    }

    private fun getStringPresentationTimeLimit(t: Long?): String {
        if (t == null)
            return "undefined"

        var millisUntilFinishedVar: Long = t

        val minutes = TimeUnit.SECONDS.toMinutes(millisUntilFinishedVar)
        millisUntilFinishedVar -= TimeUnit.MINUTES.toSeconds(minutes)

        val seconds = millisUntilFinishedVar

        return String.format(
                Locale.getDefault(),
                " %02d:%02d",
                minutes, seconds
        )
    }

    private fun checkStoragePermission(): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val arr = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (readPerm != PackageManager.PERMISSION_GRANTED || writePerm != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arr,
                    1)
            return false
        }
        return true
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
