package ru.spb.speech.appSupport

import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import ru.spb.speech.database.PresentationData

class TestFilesHandlerClicker(private val presentationData: PresentationData,
                              private val timeList: List<Int>) {

    val controllerActions = MutableLiveData<AudioAnalyzer.AudioAnalyzerState>()

    private val pageCount = presentationData.pageCount?:0
    private var currentPage = 1

    init { startClickHandler() }

    private fun startClickHandler() {
        Handler().postDelayed({
            if (currentPage >= pageCount || currentPage >= timeList.size) {
                controllerActions.postValue(AudioAnalyzer.AudioAnalyzerState.FINISH)
            } else {
                currentPage++
                controllerActions.postValue(AudioAnalyzer.AudioAnalyzerState.NEXT_SLIDE)
                startClickHandler()
            }
        }, timeList[currentPage-1] * 1000L - (
                if (currentPage != 1) timeList[currentPage-2] * 1000L else 2500L) + 2500L)
    }
}