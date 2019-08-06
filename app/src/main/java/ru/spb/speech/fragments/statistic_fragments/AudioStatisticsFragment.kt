package ru.spb.speech.fragments.statistic_fragments

import android.annotation.SuppressLint
import android.view.View
import kotlinx.android.synthetic.main.fragment_audiostatistics.view.*
import kotlinx.android.synthetic.main.item_slide_info.view.*
import ru.spb.speech.R
import ru.spb.speech.appSupport.toAllStatisticsInfo
import ru.spb.speech.appSupport.toDefaultStringFormat
import ru.spb.speech.database.getTrainingLenInSec
import ru.spb.speech.database.toSlideInfoList
import ru.spb.speech.fragments.StatisticsFragment

class AudioStatisticsFragment : StatisticsFragment() {

    override val fragmentLayoutId = R.layout.fragment_audiostatistics

    @SuppressLint("SetTextI18n")
    override fun onViewInflated(view: View) {

        val training   = speechDb
                .TrainingDataDao()
                .getTrainingWithId(trainingId)

        val tsd = slideDBHelper
                .getAllSlidesForTraining(training)
                ?: return

        val trainingLenInSec = tsd.getTrainingLenInSec()
        val slideInfoList = tsd.toSlideInfoList()


        with (slideInfoList.toAllStatisticsInfo()) {
            view.apply {
                tv_slide_number.visibility = View.GONE

                tv_sum_pause_len.text = "${getString(R.string.silence_percentage_on_slide)}: ${
                (this@with.silencePercentage).toDefaultStringFormat()} ${
                getString(R.string.seconds)}\n${(this@with.silencePercentage/trainingLenInSec * 100)
                        .toDefaultStringFormat()}${getString(R.string.percent_of_training)}"
                tv_average_pause_len.text = "${getString(R.string.average_pause_length)}: ${
                (this@with.pauseAverageLength.toDouble() / 10000).toDefaultStringFormat()} ${
                getString(R.string.seconds)}"
                tv_count_pause.text = "${getString(R.string.long_pauses_amount)}: ${
                this@with.longPausesAmount}"
            }
        }
        with (view) {
            btn_show_all_info.setOnClickListener {
                AllAudioStatisticsDialogFragment.instance(slideInfoList).show(childFragmentManager, "")
            }
        }
    }
}