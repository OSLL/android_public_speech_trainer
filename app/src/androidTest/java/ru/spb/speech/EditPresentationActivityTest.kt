package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import org.junit.*
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import android.widget.NumberPicker
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.UiController
import android.support.test.espresso.ViewAction
import android.view.View
import org.hamcrest.Matcher
import android.support.test.espresso.matcher.ViewMatchers.withClassName
import android.support.test.espresso.Espresso.onView
import org.hamcrest.Matchers


@RunWith(AndroidJUnit4::class)
class EditPresentationActivityTest : BaseInstrumentedTest() {

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)
    lateinit var helper: TestHelper

    @Before
    fun enableDebugMode() {
        helper = TestHelper(activityTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }

    @Test
    fun datePickerExist() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.datePicker)).check(matches(isDisplayed()))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
    }

    @Test
    fun setDateForPresentation() {
        // Изменение даты при добавлении
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.datePicker)).perform(PickerActions.setDate(2035, 5, 12))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withText("2035-5-12")).check(matches(isDisplayed()))

        // Изменение даты при редактировании
        onView(withText("2035-5-12")).perform(longClick())
        sleep(2000)
        onView(withText(R.string.edit)).perform(click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        sleep(2000)
        onView(withId(R.id.datePicker)).perform(PickerActions.setDate(2036, 5, 12))
        onView(withId(R.id.addPresentation)).perform(click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        sleep(2000)
        onView(withText("2036-5-12")).check(matches(isDisplayed()))
    }

    @Test
    fun setNameOfPresentation() {
        // Изменение названия презентации при добавлении
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.presentationName)).perform(replaceText(activityTestRule.activity.getString(R.string.first_debug_presentation_name)))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
        onView(withText(R.string.first_debug_presentation_name)).check(matches(isDisplayed()))

        // Изменение названия презентации при редактировании
        onView(withText(R.string.first_debug_presentation_name)).perform(longClick())
        sleep(2000)
        onView(withText(R.string.edit)).perform(click())
        sleep(2000)
        onView(withId(R.id.presentationName)).perform(replaceText(activityTestRule.activity.getString(R.string.second_debug_presentation_name)))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
        onView(withText(R.string.second_debug_presentation_name)).check(matches(isDisplayed()))

        onView(withText(R.string.second_debug_presentation_name)).perform(longClick())
        sleep(2000)
        onView(withText(R.string.edit)).perform(click())
        sleep(2000)
        onView(withId(R.id.presentationName)).perform(replaceText(activityTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
    }

    @Test
    fun setDurationOfPresentation() {
        // Изменение длительности презентации при добавлении
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.numberPicker1)).perform(setNumber(activityTestRule.activity.resources.getInteger(R.integer.one_minute)))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
        onView(withText(R.string.first_debug_time)).check(matches(isDisplayed()))

        // Изменение длительности презентации при редактировании
        onView(withText(R.string.first_debug_time)).perform(longClick())
        sleep(2000)
        onView(withText(R.string.edit)).perform(click())
        sleep(2000)
        onView(withId(R.id.numberPicker1)).perform(setNumber(activityTestRule.activity.resources.getInteger(R.integer.two_minutes)))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
        onView(withText(R.string.second_debug_time)).check(matches(isDisplayed()))
    }

    private fun setNumber(number: Int): ViewAction {
        return object : ViewAction {
            override fun perform(uiController: UiController, view:View) {
                val numberPicker = view as NumberPicker
                numberPicker.value = number
            }

            override fun getDescription(): String {
                return activityTestRule.activity.getString(R.string.set_number_into_NP)           }

            override fun getConstraints(): Matcher <View> {
                return isAssignableFrom(NumberPicker::class.java)
            }
        }
    }
}