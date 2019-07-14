package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.util.Log
import ru.spb.speech.database.SpeechDataBase
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugSlidesTest : BaseInstrumentedTest() {
    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var helper: TestHelper
    lateinit var uiDevice: UiDevice

    @Before
    fun enableDebugMode() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        helper = TestHelper(mIntentsTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }

    @Test
    fun test(){
        val db = SpeechDataBase.getInstance(getTargetContext())?.PresentationDataDao()
        db?.deleteAll() // удаление всех элементов БД
        assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту
        onView(withId(R.id.addBtn)).perform(ViewActions.click())

        onView(withText(R.string.making_presentation)).check(matches(isDisplayed()))
        onView(withText("26")).check(matches(isDisplayed()))

        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        Thread.sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_until_you_can_switch_to_workout_statistics).toLong())
    }


    @Test
    fun exportStasisticsFlagTest(){
        helper.changeExportStatisticsFlag()
        
        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withId(R.id.presentationName)).perform(ViewActions.replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        Thread.sleep(2000)
        helper.startTrainingDialog(uiDevice)
        Thread.sleep(2000)
        uiDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()
        Thread.sleep(2000)
        onView(withId(android.R.id.button1)).perform(ViewActions.click())


        Thread.sleep(2000)
        onView(withId(R.id.export)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        Thread.sleep(2000)
        uiDevice.pressBack()
        Thread.sleep(2000)
        
        helper.changeExportStatisticsFlag()
    }

    @Test
    fun checkNoWordsTraining(){
        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withId(R.id.presentationName)).perform(ViewActions.replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        Thread.sleep(2000)
        helper.startTrainingDialog(uiDevice)
        Thread.sleep(2000)
        uiDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()
        Thread.sleep(2000)
        onView(withId(android.R.id.button1)).perform(ViewActions.click())


        Thread.sleep(2000)
        onView(withId(R.id.earnOfTrain)).check(matches(withText(containsString("0.0"))))
        Thread.sleep(2000)

        uiDevice.pressBack()
        Thread.sleep(2000)
    }

}
