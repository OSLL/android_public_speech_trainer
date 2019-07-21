package ru.spb.speech.fragments.audiostatistics_fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_audiostatistics.view.*
import kotlinx.android.synthetic.main.item_slide_info.view.*
import ru.spb.speech.R
import ru.spb.speech.appSupport.toAllStatisticsInfo
import ru.spb.speech.appSupport.toDefaultStringFormat
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.getTrainingLenInSec
import ru.spb.speech.database.helpers.TrainingSlideDBHelper
import ru.spb.speech.database.toSlideInfoList

class AudioStatisticsFragment : Fragment() {

    companion object {
        const val LOG = "audio_statistics_fragment_log"
        private const val EXTRA_AUDIO_STATISTICS_TRAINING_ID = "extra_audio_statistics_presentation_id"
        val instance = { presentationID: Int ->
            AudioStatisticsFragment().apply {
                arguments = Bundle()
                        .apply { putInt(EXTRA_AUDIO_STATISTICS_TRAINING_ID, presentationID) }
            }
        }
    }

    private val trainingId by lazy { arguments!!.getInt(EXTRA_AUDIO_STATISTICS_TRAINING_ID) }

    private val speechDb by lazy { SpeechDataBase.getInstance(activity!!)!! }

    private val slideDBHelper by lazy { TrainingSlideDBHelper(activity!!) }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_audiostatistics, container, false)

        val training   = speechDb
                .TrainingDataDao()
                .getTrainingWithId(trainingId)

        val tsd = slideDBHelper
                .getAllSlidesForTraining(training)
                ?: return null

        val trainingLenInSec = tsd.getTrainingLenInSec()
        val slideInfoList = tsd.toSlideInfoList()

        with (slideInfoList.toAllStatisticsInfo()) {
            v.apply {
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

        with (v) {
            btn_show_all_info.setOnClickListener {
                AllAudioStatisticsDialogFragment.instance(slideInfoList).show(childFragmentManager, "")
            }
        }

        return v
    }
}