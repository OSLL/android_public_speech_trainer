package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import android.support.test.espresso.matcher.ViewMatchers.*
import org.junit.After
import ru.spb.speech.database.SpeechDataBase


@RunWith(AndroidJUnit4::class)
class DisplayOfAssessmentInformationTest : BaseInstrumentedTest(){
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
    fun after(){
        mDevice.pressBack()
        mDevice.pressBack()

        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun displayInformation() {
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.presentationName)).perform(replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_to_display_presentation).toLong())

        helper.startTrainingDialog(mDevice)

        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_to_load_the_workout_page).toLong())
        mDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()
        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_until_you_can_switch_to_workout_statistics).toLong())
        onView(withId(android.R.id.button1)).perform(click())

        onView(withId(R.id.question)).perform(click())
        onView(withText(R.string.assessment_for_training_title)).check(matches(isDisplayed()))
    }
}
