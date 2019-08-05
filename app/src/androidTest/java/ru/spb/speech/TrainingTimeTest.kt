package ru.spb.speech

import android.preference.PreferenceManager
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
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class TrainingTimeTest : BaseInstrumentedTest() {
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
    fun FullTrainTest(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())

        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withId(R.id.presentationName)).perform(ViewActions.replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        Thread.sleep(2000)
        helper.startTrainingDialog(uiDevice)

        val startTime = System.nanoTime()

        for (i in 1..26) {
            Thread.sleep(6000)
            uiDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.next))).click()
        }

        uiDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()

        val estimatedTime = System.nanoTime() - startTime
        onView(withId(android.R.id.button1)).perform(ViewActions.click())

        val realTime = sharedPreferences.getInt(mIntentsTestRule.activity.getString(R.string.training_duration), -1).toLong()

        if (abs(realTime - estimatedTime) > 5){
            assertEquals(realTime, estimatedTime)
        }



    }

    @Test
    fun earlyEndTraining(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())

        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withId(R.id.presentationName)).perform(ViewActions.replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        Thread.sleep(2000)
        helper.startTrainingDialog(uiDevice)

        val startTime = System.nanoTime()
        Thread.sleep(6000)
        uiDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()

        val estimatedTime = System.nanoTime() - startTime

        onView(withId(android.R.id.button1)).perform(ViewActions.click())

        val realTime = sharedPreferences.getInt(mIntentsTestRule.activity.getString(R.string.training_duration), -1).toLong()

        if (abs(realTime - estimatedTime) > 5){
            assertEquals(realTime, estimatedTime)
        }
    }

}