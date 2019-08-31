package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.util.Log
import ru.spb.speech.database.SpeechDataBase
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.spb.speech.appSupport.TrainingStatisticsData
import ru.spb.speech.database.helpers.TrainingDBHelper
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TrainingTimeTest : BaseInstrumentedTest() {
    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    val SLIDES_COUNT_IN_DEB_PRES = 26L
    val TIME_PER_SLIDE = 6000L

    val TRAINING_TIME_FULL = SLIDES_COUNT_IN_DEB_PRES * TIME_PER_SLIDE
    val TRAINING_TIME_EATLY_STOP = 15000L

    val DELTA = 0.1

    lateinit var helper: TestHelper
    lateinit var device: UiDevice
    lateinit var presentationName: String

    @Before
    fun enableDebugMode() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        helper = TestHelper(mIntentsTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }

    @Test
    fun timerPresEnded() {

        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withId(R.id.presentationName)).perform(ViewActions.replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        Thread.sleep(2000)
        helper.startTrainingDialog(device)

        for (i in 1..SLIDES_COUNT_IN_DEB_PRES) {
            Thread.sleep(TIME_PER_SLIDE)
            device.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.next))).click()
        }

        device.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).clickAndWaitForNewWindow()
        device.pressBack()
        device.pressBack()

        assertEquals(getTrainingTimeInMillis()!!.toFloat(), TRAINING_TIME_FULL.toFloat(), (TRAINING_TIME_FULL * DELTA).toFloat())
    }

    @Test
    fun timerEarlyStop() {

        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withId(R.id.presentationName)).perform(ViewActions.replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        Thread.sleep(2000)
        helper.startTrainingDialog(device)

        Thread.sleep(TRAINING_TIME_EATLY_STOP / 2)

        device.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.next))).click()

        Thread.sleep(TRAINING_TIME_EATLY_STOP / 2)

        device.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).clickAndWaitForNewWindow()
        device.pressBack()
        device.pressBack()

        assertEquals(getTrainingTimeInMillis()!!.toFloat(), TRAINING_TIME_EATLY_STOP.toFloat(), (TRAINING_TIME_EATLY_STOP * DELTA).toFloat())
    }

    private fun getTrainingTimeInMillis(): Long? {
        val lastPresentation = SpeechDataBase.getInstance(mIntentsTestRule.activity)?.PresentationDataDao()?.getLastPresentation()
        val lastTraining = TrainingDBHelper(mIntentsTestRule.activity).getAllTrainingsForPresentation(lastPresentation!!)!!.last()
        val trainingTime = TrainingStatisticsData(mIntentsTestRule.activity, lastPresentation, lastTraining).currentTrainingTime
        return TimeUnit.SECONDS.toMillis(trainingTime)
    }

}