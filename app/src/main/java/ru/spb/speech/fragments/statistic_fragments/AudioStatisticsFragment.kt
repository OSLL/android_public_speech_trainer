package ru.spb.speech.fragments.statistic_fragments

import android.annotation.SuppressLint
import android.view.View
import kotlinx.android.synthetic.main.fragment_audiostatistics.view.*
import kotlinx.android.synthetic.main.item_slide_info.view.*
import ru.spb.speech.R
import ru.spb.speech.appSupport.getAverage
import ru.spb.speech.appSupport.toDefaultStringFormat
import ru.spb.speech.database.toSlideInfoList
import ru.spb.speech.fragments.StatisticsFragment

class AudioStatisticsFragment : StatisticsFragment() {

    override val fragmentLayoutId = R.layout.fragment_audiostatistics

    @SuppressLint("SetTextI18n")
    override fun onViewInflated(view: View) {

        val slideInfoList = trainingSlideList?.toSlideInfoList() ?: return

        with (slideInfoList.getAverage()) {
            view.apply {
                tv_slide_number.visibility = View.GONE

                tv_sum_pause_len.text = "${getString(R.string.silence_percentage_on_slide)}: ${
                (this@with.silencePercentage * 10).toDefaultStringFormat()} ${
                getString(R.string.seconds)}"

                tv_average_pause_len.text = "${getString(R.string.average_pause_length)}: ${
                (this@with.pauseAverageLength.toDouble() / 1000).toDefaultStringFormat()} ${
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