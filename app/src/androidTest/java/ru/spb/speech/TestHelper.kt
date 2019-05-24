package ru.spb.speech

import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import com.xwray.groupie.ViewHolder
import ru.spb.speech.TestHelper.TestConstants.allText
import ru.spb.speech.TestHelper.TestConstants.firstSlideText
import ru.spb.speech.TestHelper.TestConstants.secondSlideText
import ru.spb.speech.TestHelper.TestConstants.slide1spentTime
import ru.spb.speech.TestHelper.TestConstants.slide2spentTime
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.TrainingData
import ru.spb.speech.database.TrainingSlideData
import ru.spb.speech.database.helpers.TrainingDBHelper
import ru.spb.speech.database.helpers.TrainingSlideDBHelper

class TestHelper(private val activity: Activity) {

    fun setTrainingPresentationMod(mode: Boolean) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getTargetContext())
        val spe = sp.edit()
        val testPresentationMode = activity.getString(R.string.deb_pres)
        val testPresentationAudio = activity.getString(R.string.deb_speech_audio_key)

        spe.putBoolean(testPresentationMode, mode)
        spe.putBoolean(testPresentationAudio, mode)
        spe.apply()

        if (sp.getBoolean(activity.getString(R.string.first_run), true)) {
            Espresso.onView(ViewMatchers.withText(activity.getString(R.string.good)))
                    .perform(ViewActions.click())
            spe.putBoolean(activity.getString(R.string.first_run), false).apply()
        }
    }

    fun removeDebugSlides(){
        Espresso.onView(ViewMatchers.withText("making_presentation")).
                perform(ViewActions.longClick()) // Вызов диалогового окна удаления презентации

        // Нажатие на кнопку "удалить"
        Espresso.onView(ViewMatchers.withText(activity.getString(R.string.remove)))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())
    }

    fun addDebugPresentation(presentationName: String = "", notifications: Boolean = false): String {
        var name = presentationName
        if (name == "")
            name = activity.getString(R.string.deb_pres_name).split(".")[0]

        Espresso.onView(ViewMatchers.withId(R.id.addBtn)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.presentationName)).perform(ViewActions.clearText(), ViewActions.typeText(name), ViewActions.closeSoftKeyboard())
        if (notifications == true)
            Espresso.onView(ViewMatchers.withId(R.id.notifications)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.addPresentation)).perform(ViewActions.click())

        return name
    }

    fun removePresentationFromRecyclerView(position: Int) {
        Espresso.onView(ViewMatchers.withId(R.id.recyclerview_startpage))
                .perform(RecyclerViewActions
                        .actionOnItemAtPosition<ViewHolder>(position, ViewActions.longClick()))

        // Нажатие на кнопку "удалить"
        Espresso.onView(ViewMatchers.withText(InstrumentationRegistry.getTargetContext().getString(R.string.remove)))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())
    }

    fun addDummyStatisticsForPresentation(presentationId: Int, tContext: Context): DummyStatisticsResponse? {
        val db = SpeechDataBase.getInstance(tContext)!!
        val presentationData = db.PresentationDataDao().getPresentationWithId(presentationId) ?: return null
        val trainingHelper = TrainingDBHelper(tContext)
        val slidesHelper = TrainingSlideDBHelper(tContext)

        var training1 = TrainingData()
        training1.allRecognizedText = allText
        training1.timeStampInSec = slide1spentTime + slide2spentTime

        trainingHelper.addTrainingInDB(training1, presentationData)
        training1 = db.TrainingDataDao().getLastTraining()

        val slide1 = TrainingSlideData()
        slide1.knownWords = firstSlideText
        slide1.spentTimeInSec = slide1spentTime

        val slide2 = TrainingSlideData()
        slide2.knownWords = secondSlideText
        slide2.spentTimeInSec = slide2spentTime

        slidesHelper.addTrainingSlideInDB(slide1, training1)
        slidesHelper.addTrainingSlideInDB(slide2, training1)

        return DummyStatisticsResponse(listOf(training1), slidesHelper.getAllSlidesForTraining(training1)!!)
    }

    fun startTrainingDialog(uiDevice : UiDevice) {
        Espresso.onView(ViewMatchers.withText(R.string.making_presentation)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.start_training)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        uiDevice.findObject(UiSelector().text(activity.getString(R.string.yes))).click()
    }

    private object TestConstants {
        const val slide1spentTime: Long = 5
        const val slide2spentTime: Long = 6
        const val firstSlideText = "1 2 3 4 5 6 7 8 9"
        const val secondSlideText = "a b c d e f g"

        const val allText = "$firstSlideText $secondSlideText"
    }
}

class DummyStatisticsResponse(val trainings: List<TrainingData>, val slides: List<TrainingSlideData>)
