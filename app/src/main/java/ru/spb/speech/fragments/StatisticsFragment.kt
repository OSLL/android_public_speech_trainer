package ru.spb.speech.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.helpers.TrainingSlideDBHelper

abstract class StatisticsFragment: Fragment() {

    companion object {
        protected const val LOG = "statistics_fragment_log"
        private const val EXTRA_AUDIO_STATISTICS_TRAINING_ID = "extra_statistics_presentation_id"
        val instance = {
            fragment: StatisticsFragment, presentationID: Int ->
            fragment.apply {
                arguments = Bundle()
                        .apply { putInt(EXTRA_AUDIO_STATISTICS_TRAINING_ID, presentationID) }
            }
        }
    }

    abstract val fragmentLayoutId: Int

    protected val trainingId by lazy { arguments!!.getInt(EXTRA_AUDIO_STATISTICS_TRAINING_ID) }

    protected val speechDb by lazy { SpeechDataBase.getInstance(activity!!)!! }

    protected val slideDBHelper by lazy { TrainingSlideDBHelper(activity!!) }

    protected val training by lazy {
        speechDb.TrainingDataDao().getTrainingWithId(trainingId)
    }

    protected val trainingSlideList by lazy {
        slideDBHelper.getAllSlidesForTraining(training)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val v = inflater.inflate(fragmentLayoutId, container, false)!!
        onViewInflated(v)
        return v
    }

    open fun onViewInflated(view: View) {  }
}