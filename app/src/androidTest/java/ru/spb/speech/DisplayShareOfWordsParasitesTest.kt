package ru.spb.speech

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.spb.speech.database.SpeechDataBase
import java.lang.Thread.sleep
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.util.Log
import kotlinx.android.synthetic.main.activity_training_statistics.*
import org.hamcrest.CoreMatchers.containsString
import org.junit.After


@RunWith(AndroidJUnit4::class)
class DisplayShareOfWordsParasitesTest : BaseInstrumentedTest() {

    @Rule
    @JvmField
    var mIntentsTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var helper: TestHelper
    private lateinit var mDevice: UiDevice
    private val tContext = InstrumentationRegistry.getTargetContext()
    private val db: SpeechDataBase = SpeechDataBase.getInstance(tContext)!!

    init {
        db.PresentationDataDao().deleteTestPresentations()
    }

    @Before
    fun before() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        helper = TestHelper(mIntentsTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }

    @Test
    fun displayShareOfWordsParasite(){
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.presentationName)).perform(replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(click())
        helper.startTrainingDialog(mDevice)

        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.workout_time_in_milliseconds_for_check_display_parasites).toLong())
        mDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()
        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_until_you_can_switch_to_workout_statistics).toLong())
        onView(withId(android.R.id.button1)).perform(click())
        onView(withText(containsString(mIntentsTestRule.activity.getString(R.string.word_share_of_parasites)))).perform(scrollTo()).check(matches(isDisplayed()))
        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_wait_after_test).toLong())

        mDevice.pressBack()
    }
}