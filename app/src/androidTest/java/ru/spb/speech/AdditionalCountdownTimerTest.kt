package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.widget.NumberPicker
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.UiController
import android.support.test.espresso.ViewAction
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.view.View
import org.hamcrest.Matcher
import org.junit.After


@RunWith(AndroidJUnit4::class)
class AdditionalCountdownTimerTest {
    lateinit var helper: TestHelper
    lateinit var mDevice: UiDevice

    @Before
    fun before() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        helper = TestHelper(mIntentsTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun after() {
        mDevice.pressBack()
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }

    @Rule
    @JvmField
    var mIntentsTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun additionalCountdownTimerTest(){
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.presentationName)).perform(replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.numberPicker1)).perform(setNumber(mIntentsTestRule.activity.resources.getInteger(R.integer.one_minute_report_setting)))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_to_display_presentation).toLong())

        helper.startTrainingDialog(mDevice)

        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.training_time_in_milliseconds_to_trigger_an_additional_timer).toLong())

        mDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()

        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_until_you_can_switch_to_workout_statistics).toLong())

        mDevice.pressBack()

        onView(withId(R.id.time_left)).check(matches(hasTextColor(android.R.color.holo_red_light)))
    }

    private fun setNumber(num: Int): ViewAction {
        return object : ViewAction {
            override fun perform(uiController: UiController, view: View) {
                val np = view as NumberPicker
                np.value = num

            }

            override fun getDescription(): String {
                return mIntentsTestRule.activity.getString(R.string.setNumber_function_information)
            }

            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(NumberPicker::class.java)
            }
        }
    }

}