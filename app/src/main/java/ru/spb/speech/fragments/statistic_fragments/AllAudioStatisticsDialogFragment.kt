package ru.spb.speech.fragments.statistic_fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.DividerItemDecoration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.fragment_full_audio_statisctics.view.*
import kotlinx.android.synthetic.main.item_slide_info.view.*
import ru.spb.speech.R
import ru.spb.speech.appSupport.SlideInfo
import ru.spb.speech.appSupport.toDefaultStringFormat

class AllAudioStatisticsDialogFragment: DialogFragment() {

    companion object {
        private const val EXTRA_AUDIO_STATISTICS_LIST = "extra_audio_statistics_list"
        val instance = {
            list: List<SlideInfo> ->
            AllAudioStatisticsDialogFragment().apply {
                arguments = Bundle().apply { putParcelableArray(EXTRA_AUDIO_STATISTICS_LIST, list.toTypedArray()) }
            }
        }
    }

    private val statisticsList by lazy {
        (arguments!!.getParcelableArray(EXTRA_AUDIO_STATISTICS_LIST) as Array<SlideInfo>).toList()
    }

    private val adapter by lazy { GroupAdapter<ViewHolder>() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_full_audio_statisctics, container, false)

        for (s in statisticsList)
            adapter.add(SlideInfoItem(s, ::getString))

        v.recycler_full_stat.apply {
            addItemDecoration(DividerItemDecoration(activity, LinearLayout.VERTICAL))
            this.adapter = this@AllAudioStatisticsDialogFragment.adapter
        }

        v.close.setOnClickListener { dismiss() }

        return v
    }
}

class SlideInfoItem(private val slideInfo: SlideInfo,
                    private val getString: (id: Int) -> String): Item<ViewHolder>() {

    override fun getLayout() = R.layout.item_slide_info

    @SuppressLint("SetTextI18n")
    override fun bind(viewHolder: ViewHolder, position: Int) {
        with (viewHolder.itemView) {
            tv_slide_number.text = "${getString(R.string.slide)}: ${slideInfo.slideNumber}"

            tv_sum_pause_len.text = "${getString(R.string.silence_percentage_on_slide)}: ${
            (slideInfo.silencePercentage * 10).toDefaultStringFormat()} ${
            getString(R.string.seconds)}"

            tv_average_pause_len.text = "${getString(R.string.average_pause_length)}: ${
            (slideInfo.pauseAverageLength.toDouble() / 1000).toDefaultStringFormat()} ${
            getString(R.string.seconds)}"

            tv_count_pause.text = "${getString(R.string.long_pauses_amount)}: ${
            slideInfo.longPausesAmount}"
        }
    }
}