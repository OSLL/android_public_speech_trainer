package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.longClick
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import org.junit.*
import org.junit.runner.RunWith
import java.lang.Thread.sleep

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

        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.datePicker)).perform(PickerActions.setDate(2035, 5, 12))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
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
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

    }

    @Test
    fun setDateForPresentation() {
        // Изменение даты при добавлении
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withText("2035-5-12")).check(matches(isDisplayed()))

        // Изменение даты при редактировании
        onView(withText("2035-5-12")).perform(longClick())
        onView(withText("Редактировать")).perform(click())
        sleep(2000)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withId(R.id.datePicker)).perform(PickerActions.setDate(2036, 5, 12))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withText("2036-5-12")).check(matches(isDisplayed()))
    }

}