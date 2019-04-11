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

    val SLIDES_COUNT_IN_DEB_PRES = 26L
    val TIME_PER_SLIDE = 6000L

    val TRAINING_TIME_FULL = SLIDES_COUNT_IN_DEB_PRES * TIME_PER_SLIDE
    val TRAINING_TIME_EATLY_STOP = 15000L

    val DELTA = 5000L

    init {
        grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        grantPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var helper: TestHelper
    lateinit var device: UiDevice
    lateinit var presentationName: String

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(device, CoreMatchers.notNullValue())

        helper = TestHelper(activityTestRule.activity).apply {
            setTrainingPresentationMod(true)
        }


        presentationName = activityTestRule.activity.getString(R.string.deb_pres_name).split(".")[0]
        helper.addDebPresentation(presentationName)
    }

    @After
    fun cleanUp() {
        helper.setTrainingPresentationMod(false)
        helper.removeDebugSlides()
    }

    @Test
    fun timerPresEnded() {
        device.findObject(UiSelector().text(presentationName)).apply {
            waitForExists(2000)
            clickAndWaitForNewWindow()
        }

        for (i in 1..SLIDES_COUNT_IN_DEB_PRES) {
            Thread.sleep(TIME_PER_SLIDE)
            device.findObject(UiSelector().text(activityTestRule.activity.getString(R.string.next))).click()
        }

        device.findObject(UiSelector().text(activityTestRule.activity.getString(R.string.stop))).clickAndWaitForNewWindow()
        device.pressBack()
        device.pressBack()

        assertEquals(getTrainingTimeInMillis()!!.toFloat(), TRAINING_TIME_FULL.toFloat(), DELTA.toFloat())
    }

    @Test
    fun timerEarlyStop() {
        device.findObject(UiSelector().text(presentationName)).apply {
            waitForExists(2000)
            clickAndWaitForNewWindow()
        }

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