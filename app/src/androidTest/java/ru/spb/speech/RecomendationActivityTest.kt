package ru.spb.speech

import android.content.ComponentName
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.util.Log
import android.view.View
import ru.spb.speech.database.SpeechDataBase
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecomendationActivityTest : BaseInstrumentedTest() {

    private var mDevice: UiDevice? = null
    private var presName = ""

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Before
    fun before() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(mDevice, CoreMatchers.notNullValue())

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val OnMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val OnAudio = mIntentsTestRule.activity.getString(R.string.deb_speech_audio_key)

        debSl.putBoolean(OnMode, true)
        debSl.putBoolean(OnAudio, true)
        debSl.putBoolean(mIntentsTestRule.activity.getString(R.string.useStatistics), true)

        debSl.apply()
    }

    @After
    fun after(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val OnMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val OnAudio = mIntentsTestRule.activity.getString(R.string.deb_speech_audio_key)

        debSl.putBoolean(OnMode, false)
        debSl.putBoolean(OnAudio, false)
        debSl.apply()

        mDevice!!.pressBack()

        onView(withText(presName)).perform(ViewActions.longClick())
        Thread.sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_to_display_the_delete_button).toLong())
        onView(withText(mIntentsTestRule.activity.getString(R.string.remove))).perform(ViewActions.click())
    }

    @Test
    fun textViewExists() {

        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        presName = mIntentsTestRule.activity.getString(R.string.deb_pres_name).substring (mIntentsTestRule.activity.resources.getInteger(R.integer.zero), mIntentsTestRule.activity.getString(R.string.deb_pres_name).indexOf(mIntentsTestRule.activity.getString(R.string.pdf_format)))
        onView(withId(R.id.presentationName)).perform(ViewActions.clearText(), ViewActions.typeText(presName), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        mDevice!!.findObject(UiSelector().text(presName)).click()
        Thread.sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.workout_time_in_milliseconds_for_training).toLong())
        mDevice!!.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()
        Thread.sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_until_you_can_switch_to_workout_statistics).toLong())
        onView(withId(android.R.id.button1)).perform(ViewActions.click())
        onView(withId(R.id.improve_mark_button)).perform(ViewActions.click())

        onView(withId(R.id.recomendationLabel)).check(matches(withText("Рекомендации к выступлению")))
        onView(withId(R.id.slidesTimeLabel)).check(matches(withText("Время слайдов")))
        onView(withId(R.id.slidesTimeRecomendation)).check(matches(withText("Рекомендации по времени слайдов - заглушка")))
        onView(withId(R.id.slidesFrequency)).check(matches(withText("Время слайдов")))
        onView(withId(R.id.slidesFrequencyRecomendation)).check(matches(withText("Рекомендации по частоте слов - заглушка")))
        onView(withId(R.id.scumWordsLabel)).check(matches(withText("Слова паразиты")))
        onView(withId(R.id.scumWordsRecomendation)).check(matches(withText("Рекомендации по словам-паразитам - заглушка")))
        onView(withId(R.id.backToStatistics)).check(matches(withText("Назад к статистике")))
        onView(withId(R.id.toHomeScreen)).check(matches(withText("На главную")))

        onView(withId(R.id.backToStatistics)).perform(ViewActions.click())
        onView(withId(R.id.earnOfTrain)).check(matches(isDisplayed()))

        onView(withId(R.id.improve_mark_button)).perform(ViewActions.click())
        onView(withId(R.id.toHomeScreen)).perform(ViewActions.click())
        onView(withId(R.id.addBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.addBtn)).perform(ViewActions.click())



    }
}