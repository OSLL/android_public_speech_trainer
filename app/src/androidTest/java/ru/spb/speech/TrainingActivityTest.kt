package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.spb.speech.DBTables.SpeechDataBase
import ru.spb.speech.DBTables.helpers.TrainingDBHelper
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TrainingActivityTest {

    val TRAINING_TIME_FULL = 104000L // amount of slides * 4 (time per slide ~4 sec)
    val TRAINING_TIME_EATLY_STOP = 15000L
    val DELTA = 5000L

    val SLIDES_COUNT_IN_DEB_PRES = 26

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var helper: TestHelper

    lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(device, CoreMatchers.notNullValue())

        helper = TestHelper(activityTestRule.activity).apply {
            setTrainingPresentationMod(true)
        }
    }

    @After
    fun cleanUp() {
        helper.setTrainingPresentationMod(false)
        helper.removeDebugSlides()
    }

    @Test
    fun timerPresEnded() {
        val presentationName = activityTestRule.activity.getString(R.string.deb_pres_name)
        helper.addDebPresentation(presentationName)

        device.findObject(UiSelector().text(presentationName)).clickAndWaitForNewWindow()

        for (i in 1..SLIDES_COUNT_IN_DEB_PRES) {
            Thread.sleep(TRAINING_TIME_FULL / SLIDES_COUNT_IN_DEB_PRES)
            device.findObject(UiSelector().text(activityTestRule.activity.getString(R.string.next))).click()
        }

        device.findObject(UiSelector().text(activityTestRule.activity.getString(R.string.stop))).clickAndWaitForNewWindow()
        device.pressBack()
        device.pressBack()

        assertEquals(getTrainingTimeInMillis()!!.toFloat(), TRAINING_TIME_FULL.toFloat(), DELTA.toFloat())
    }

    @Test
    fun timerEarlyStop() {
        val presentationName = activityTestRule.activity.getString(R.string.deb_pres_name)
        helper.addDebPresentation(presentationName)

        device.findObject(UiSelector().text(presentationName)).clickAndWaitForNewWindow()

        Thread.sleep(TRAINING_TIME_EATLY_STOP / 2)

        device.findObject(UiSelector().text(activityTestRule.activity.getString(R.string.next))).click()

        Thread.sleep(TRAINING_TIME_EATLY_STOP / 2)

        device.findObject(UiSelector().text(activityTestRule.activity.getString(R.string.stop))).clickAndWaitForNewWindow()
        device.pressBack()
        device.pressBack()

        assertEquals(getTrainingTimeInMillis()!!.toFloat(), TRAINING_TIME_EATLY_STOP.toFloat(), DELTA.toFloat())
    }

    private fun getTrainingTimeInMillis(): Long? {
        val lastPresentation = SpeechDataBase.getInstance(activityTestRule.activity)?.PresentationDataDao()?.getLastPresentation()
        val lastTraining = TrainingDBHelper(activityTestRule.activity).getAllTrainingsForPresentation(lastPresentation!!)!!.last()
        val trainingTime = TrainingStatisticsData(activityTestRule.activity, lastPresentation, lastTraining).currentTrainingTime
        return TimeUnit.SECONDS.toMillis(trainingTime)
    }
}